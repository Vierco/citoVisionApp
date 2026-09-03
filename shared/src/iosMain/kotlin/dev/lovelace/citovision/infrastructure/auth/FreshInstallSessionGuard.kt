package dev.lovelace.citovision.infrastructure.auth

import io.github.aakira.napier.Napier
import platform.Foundation.NSUserDefaults

/**
 * Descarta la sesión heredada de una instalación anterior (SECURITY_MOBILE §Tokens y §Persistencia).
 *
 * En iOS el **Keychain no forma parte del sandbox de la app**: al desinstalarla, el sistema borra su
 * contenedor —base de Room, DataStore, ficheros y `NSUserDefaults`— pero **conserva sus ítems del
 * Keychain**, y una reinstalación con el mismo *bundle id* recupera el acceso a ellos. Sin esta
 * comprobación, reinstalar la app dejaba dentro la sesión iniciada del usuario anterior.
 *
 * La detección se apoya justo en esa asimetría: la marca vive en `NSUserDefaults`, que **sí** se borra
 * al desinstalar. Si la marca no está, se trata de una instalación nueva y el Keychain se limpia antes
 * de que nadie llegue a leer la sesión.
 *
 * Depende de [KeychainStorage] y no de [KeychainTokenStore] por dos motivos: aquí solo hace falta
 * borrar —no deserializar nada— y el acceso al Keychain es sincrónico, así que puede invocarse durante
 * el arranque del proceso, antes de que exista el grafo de Koin.
 *
 * Nada de lo que se registra identifica al usuario ni contiene tokens (AGENTS.md §11).
 */
class FreshInstallSessionGuard internal constructor(
    private val storage: KeychainStorage,
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) {
    /** Constructor de producción: el ítem real del Keychain de la app. */
    constructor() : this(SecItemKeychainStorage())

    /**
     * Idempotente: en cuanto queda escrita la marca, las siguientes llamadas no vuelven a tocar el
     * Keychain. Debe invocarse en el arranque del proceso, antes de restaurar sesión (SPEC-0001 RF-8).
     */
    fun clearSessionIfReinstalled() {
        if (defaults.boolForKey(INSTALL_MARK_KEY)) return
        storage.delete()
        defaults.setBool(true, INSTALL_MARK_KEY)
        Napier.i("Instalación nueva: se descarta la sesión heredada del Keychain", tag = "Auth")
    }

    private companion object {
        /** Va con el prefijo del bundle para no chocar con otras claves de `NSUserDefaults`. */
        const val INSTALL_MARK_KEY = "dev.lovelace.citovision.install.marker"
    }
}
