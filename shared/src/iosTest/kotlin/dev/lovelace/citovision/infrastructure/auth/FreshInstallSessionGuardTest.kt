package dev.lovelace.citovision.infrastructure.auth

import platform.Foundation.NSUserDefaults
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests de [FreshInstallSessionGuard], el guardián del ADR-0009.
 *
 * Es un solo `if`, pero decide algo que no admite fallo: si al reinstalar la app entras con la sesión
 * que dejó **otra instalación** —potencialmente de otra persona— o con la sesión limpia. Ese es el
 * motivo por el que aquí sí se añaden tests, pese a la decisión general de no perseguir cobertura.
 *
 * Se cubren los dos errores posibles, simétricos y ambos graves:
 *
 * - No purgar cuando toca → el usuario hereda la sesión anterior (el fallo que originó el ADR).
 * - Purgar cuando no toca → la sesión se pierde en **cada arranque** y la app pide login siempre.
 *
 * El Keychain va con un doble (ver [KeychainTokenStoreTest] para el porqué), pero `NSUserDefaults` es
 * el **real**: es la mitad del mecanismo de detección y sí funciona en el simulador. Se usa un *suite*
 * propio, que se limpia antes y después de cada caso porque persiste entre ejecuciones.
 */
class FreshInstallSessionGuardTest {
    private val defaults = NSUserDefaults(suiteName = TEST_DEFAULTS_SUITE)

    @BeforeTest
    fun setUp() {
        defaults.removePersistentDomainForName(TEST_DEFAULTS_SUITE)
    }

    @AfterTest
    fun tearDown() {
        defaults.removePersistentDomainForName(TEST_DEFAULTS_SUITE)
    }

    @Test
    fun `given a fresh install when the guard runs then the inherited session is discarded`() {
        // Given: el Keychain conserva la sesión de la instalación anterior (no se borra al desinstalar)
        // y `NSUserDefaults` está vacío, que es justo lo que delata la reinstalación.
        val storage = FakeKeychainStorage(INHERITED_SESSION)

        // When
        FreshInstallSessionGuard(storage, defaults).clearSessionIfReinstalled()

        // Then
        assertNull(storage.storedValue)
        assertEquals(1, storage.deleteCount)
    }

    @Test
    fun `given the guard already ran when it runs again then the session is kept`() {
        // Given: un primer arranque que deja escrita la marca de instalación...
        val storage = FakeKeychainStorage()
        FreshInstallSessionGuard(storage, defaults).clearSessionIfReinstalled()
        // ...y un login posterior del usuario.
        storage.write(INHERITED_SESSION)

        // When: cualquier arranque siguiente.
        FreshInstallSessionGuard(storage, defaults).clearSessionIfReinstalled()

        // Then: la sesión sigue ahí y no ha habido un segundo borrado. Si esta aserción cae, la app
        // pide login en cada arranque.
        assertEquals(INHERITED_SESSION, storage.storedValue)
        assertEquals(1, storage.deleteCount)
    }

    @Test
    fun `given the mark was written when another defaults instance reads it then it is still there`() {
        // Given: el primer arranque escribe la marca.
        FreshInstallSessionGuard(FakeKeychainStorage(), defaults).clearSessionIfReinstalled()

        // When: el arranque siguiente es otro proceso, así que lee `NSUserDefaults` desde cero. Esto es
        // lo que comprueba que la marca se **persiste** de verdad y no vive solo en memoria.
        val storage = FakeKeychainStorage(INHERITED_SESSION)
        val guard = FreshInstallSessionGuard(storage, NSUserDefaults(suiteName = TEST_DEFAULTS_SUITE))
        guard.clearSessionIfReinstalled()

        // Then
        assertEquals(INHERITED_SESSION, storage.storedValue)
    }

    private companion object {
        /**
         * No puede ser el *bundle identifier* de la app: `initWithSuiteName:` lo prohíbe expresamente.
         * Al ser un dominio aparte, la marca real de producción nunca se toca.
         */
        const val TEST_DEFAULTS_SUITE = "dev.lovelace.citovision.tests.freshinstall"

        /** El guardián no interpreta el contenido: solo decide si borrarlo. */
        const val INHERITED_SESSION = "sesion-de-la-instalacion-anterior"
    }
}
