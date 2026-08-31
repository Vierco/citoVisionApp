package dev.lovelace.citovision.infrastructure.auth

import io.github.aakira.napier.Napier
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
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
 * [TokenStore] de iOS sobre el **Keychain**, tal y como exige SECURITY_MOBILE §Tokens ("iOS: Keychain")
 * y para que la sesión sobreviva a los reinicios (SPEC-0001 RF-8, ADR-0006).
 *
 * La sesión se serializa a JSON y se guarda como un único ítem `kSecClassGenericPassword`. Se protege
 * con `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`: accesible en segundo plano tras el primer
 * desbloqueo, pero **sin salir del dispositivo** (no viaja a backups de iCloud ni a otros equipos).
 *
 * Ningún valor de token se escribe en logs (RNF-5, AGENTS.md §11): los errores se registran solo por su
 * código `OSStatus`.
 */
@OptIn(ExperimentalForeignApi::class)
class KeychainTokenStore(
    private val service: String = DEFAULT_SERVICE,
    private val account: String = DEFAULT_ACCOUNT,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : TokenStore {
    override suspend fun load(): StoredSession? {
        val stored = readRawSession() ?: return null
        return try {
            json.decodeFromString(StoredSession.serializer(), stored)
        } catch (e: SerializationException) {
            // Formato antiguo o dato corrupto: se descarta para forzar un login limpio.
            Napier.w("Sesión guardada ilegible; se descarta", e, tag = "Auth")
            clear()
            null
        }
    }

    override suspend fun save(session: StoredSession) {
        // Serializador explícito: la sobrecarga reificada de `encodeToString` exige un import extra y aquí
        // no aporta nada.
        val payload = json.encodeToString(StoredSession.serializer(), session)
        // El Keychain no sobrescribe con `SecItemAdd`: se borra el ítem previo y se inserta el nuevo.
        clear()
        memScoped {
            val data = payload.toNSData() ?: return
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

    override suspend fun clear() {
        memScoped {
            val query = newQuery()
            SecItemDelete(query)
            CFRelease(query)
        }
    }

    private fun readRawSession(): String? =
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
