package dev.lovelace.citovision.infrastructure.auth

import io.github.aakira.napier.Napier
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * Acceso al ítem del Keychain donde vive la sesión, reducido a leer, escribir y borrar una cadena.
 *
 * Existe para separar **el interop con Security.framework** de **la lógica que decide qué hacer con el
 * dato**, que es la que vive en [KeychainTokenStore] y [FreshInstallSessionGuard]. Esa separación no es
 * cosmética: el Keychain **no está disponible para un binario de test de Kotlin/Native**. En el
 * simulador responde `errSecNotAvailable` (-25291) porque el proceso no es una app con
 * *application-identifier*, y si se le firman los *entitlements* que lo arreglarían, el simulador mata
 * el proceso. Sin esta costura, las dos clases de arriba serían imposibles de probar.
 *
 * El interop en sí queda cubierto por la verificación manual en dispositivo (ADR-0006): si fallara, la
 * sesión no sobreviviría a un reinicio de la app.
 */
internal interface KeychainStorage {
    fun read(): String?

    /** Sustituye el valor guardado, si lo hubiera. */
    fun write(value: String)

    /** Idempotente: borrar un ítem inexistente no es un error. */
    fun delete()
}

/**
 * Implementación real sobre `SecItem*`. Guarda un único ítem `kSecClassGenericPassword` identificado
 * por servicio y cuenta, protegido con `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`: accesible en
 * segundo plano tras el primer desbloqueo, pero **sin salir del dispositivo** (no viaja a backups de
 * iCloud ni a otros equipos), como exige SECURITY_MOBILE §Tokens.
 *
 * Ningún valor se escribe en logs (RNF-5, AGENTS.md §11): los errores se registran solo por su código
 * `OSStatus`.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class SecItemKeychainStorage(
    private val service: String = DEFAULT_SERVICE,
    private val account: String = DEFAULT_ACCOUNT,
) : KeychainStorage {
    override fun read(): String? =
        memScoped {
            val query = newQuery()
            CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
            CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            CFRelease(query)
            if (status != ERR_SEC_SUCCESS) return@memScoped null
            // CFBridgingRelease se queda con la referencia devuelta (regla "Copy" de CoreFoundation).
            val data = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null
            NSString.create(data = data, encoding = NSUTF8StringEncoding) as String?
        }

    override fun write(value: String) {
        // El Keychain no sobrescribe con `SecItemAdd`: hay que borrar el ítem previo e insertar el nuevo.
        delete()
        memScoped {
            val data = value.toNSData() ?: return
            val cfData = CFBridgingRetain(data)
            val query = newQuery()
            CFDictionaryAddValue(query, kSecValueData, cfData)
            CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
            val status = SecItemAdd(query, null)
            CFRelease(query)
            cfData?.let { CFRelease(it) }
            if (status != ERR_SEC_SUCCESS) {
                Napier.w("No se pudo guardar la sesión en el Keychain (OSStatus=$status)", tag = "Auth")
            }
        }
    }

    override fun delete() {
        memScoped {
            val query = newQuery()
            SecItemDelete(query)
            CFRelease(query)
        }
    }

    /** Consulta base que identifica el ítem: misma clase, servicio y cuenta en todas las operaciones. */
    private fun newQuery(): CFMutableDictionaryRef? {
        val query =
            CFDictionaryCreateMutable(
                null,
                QUERY_CAPACITY,
                kCFTypeDictionaryKeyCallBacks.ptr,
                kCFTypeDictionaryValueCallBacks.ptr,
            )
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, service.toCFStringRetained())
        CFDictionaryAddValue(query, kSecAttrAccount, account.toCFStringRetained())
        return query
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun String.toNSData(): NSData? = (this as NSString).dataUsingEncoding(NSUTF8StringEncoding)

    /**
     * El diccionario retiene sus valores (`kCFTypeDictionaryValueCallBacks`) y se libera entero con
     * `CFRelease`, así que estas cadenas puente no se liberan por separado.
     */
    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun String.toCFStringRetained(): CFTypeRef? = CFBridgingRetain(this as NSString)

    private companion object {
        const val DEFAULT_SERVICE = "dev.lovelace.citovision.auth"
        const val DEFAULT_ACCOUNT = "firebase-session"
        const val QUERY_CAPACITY = 6L

        /** Valor documentado de `errSecSuccess`; la constante no se expone en el interop de Kotlin. */
        const val ERR_SEC_SUCCESS = 0
    }
}
