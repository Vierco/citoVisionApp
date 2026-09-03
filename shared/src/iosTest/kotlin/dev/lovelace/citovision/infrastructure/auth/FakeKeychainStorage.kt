package dev.lovelace.citovision.infrastructure.auth

/**
 * [KeychainStorage] en memoria para los tests de [KeychainTokenStore] y [FreshInstallSessionGuard].
 *
 * Cuenta los borrados además de guardar el valor porque las dos cosas no significan lo mismo: un
 * `storedValue` a `null` puede querer decir "se purgó" o "nunca hubo nada", y varios de los tests
 * dependen justo de distinguirlas. Sin [deleteCount], un test de purgado pasaría también si el
 * purgado no llegara a ocurrir.
 */
internal class FakeKeychainStorage(
    initialValue: String? = null,
) : KeychainStorage {
    var storedValue: String? = initialValue
        private set

    var deleteCount: Int = 0
        private set

    override fun read(): String? = storedValue

    override fun write(value: String) {
        storedValue = value
    }

    override fun delete() {
        storedValue = null
        deleteCount++
    }
}
