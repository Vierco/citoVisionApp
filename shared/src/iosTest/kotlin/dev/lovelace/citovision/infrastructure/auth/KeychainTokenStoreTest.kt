package dev.lovelace.citovision.infrastructure.auth

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests de [KeychainTokenStore], la implementación iOS de [TokenStore] (AGENTS.md §10,
 * `ios actual impl → iosTest`).
 *
 * Se prueba contra un [FakeKeychainStorage] y no contra el Keychain real, porque **el Keychain no está
 * disponible para un binario de test de Kotlin/Native**: responde `errSecNotAvailable` (-25291), y
 * firmarle los *entitlements* que lo arreglarían hace que el simulador mate el proceso. Lo que se
 * verifica aquí es la lógica de la clase —serializar, y qué hacer cuando el dato guardado no se puede
 * leer—, que es donde están las decisiones; el interop con `SecItem*` vive en [SecItemKeychainStorage]
 * y queda cubierto por la verificación en dispositivo real (ADR-0006).
 *
 * Cada test crea su propio almacén: no hay estado compartido entre casos.
 */
class KeychainTokenStoreTest {
    @Test
    fun `given nothing stored when loading then returns null`() =
        runTest {
            // Given
            val store = KeychainTokenStore(FakeKeychainStorage())

            // When / Then
            assertNull(store.load())
        }

    @Test
    fun `given a saved session when loading then returns the same session`() =
        runTest {
            // Given
            val store = KeychainTokenStore(FakeKeychainStorage())
            val session = session()

            // When
            store.save(session)

            // Then
            assertEquals(session, store.load())
        }

    @Test
    fun `given a session without optional fields when loading then returns the same session`() =
        runTest {
            // Given: un login de invitado o por email no trae necesariamente perfil de Google.
            val store = KeychainTokenStore(FakeKeychainStorage())
            val session = session(email = null, displayName = null, photoUrl = null)

            // When
            store.save(session)

            // Then
            assertEquals(session, store.load())
        }

    @Test
    fun `given a stored session when saving another then the last one replaces it`() =
        runTest {
            // Given: al renovar el token no puede quedar rastro del anterior.
            val store = KeychainTokenStore(FakeKeychainStorage())
            store.save(session(uid = "uid-antiguo"))

            // When
            val renewed = session(uid = "uid-nuevo")
            store.save(renewed)

            // Then
            assertEquals(renewed, store.load())
        }

    @Test
    fun `given a stored session when clearing then nothing remains`() =
        runTest {
            // Given
            val storage = FakeKeychainStorage()
            val store = KeychainTokenStore(storage)
            store.save(session())

            // When
            store.clear()

            // Then
            assertNull(store.load())
            assertNull(storage.storedValue)
        }

    @Test
    fun `given corrupt data when loading then returns null and discards the item`() =
        runTest {
            // Given: algo que no es ni JSON, como si el ítem se hubiera escrito mal o dañado.
            val storage = FakeKeychainStorage("esto no es JSON")
            val store = KeychainTokenStore(storage)

            // When
            val restored = store.load()

            // Then: además de no devolver sesión, el ítem tiene que desaparecer. Si se quedara, el
            // usuario arrastraría un dato ilegible en cada arranque.
            assertNull(restored)
            assertNull(storage.storedValue)
            assertEquals(1, storage.deleteCount)
        }

    @Test
    fun `given a session in an old format when loading then returns null and discards the item`() =
        runTest {
            // Given: JSON válido, pero sin los campos que el modelo actual exige (formato anterior).
            val storage = FakeKeychainStorage("""{"idToken":"un-token"}""")
            val store = KeychainTokenStore(storage)

            // When / Then
            assertNull(store.load())
            assertNull(storage.storedValue)
            assertEquals(1, storage.deleteCount)
        }

    @Test
    fun `given a session with an unknown field when loading then it is ignored`() =
        runTest {
            // Given: un campo que una versión posterior podría añadir. `ignoreUnknownKeys` está puesto
            // justo para esto; sin él, ampliar el modelo echaría de la sesión a quien ya la tuviera.
            val storage =
                FakeKeychainStorage(
                    """
                    {"idToken":"un-token","refreshToken":"un-refresh","expiresAtEpochSeconds":1767225600,
                     "uid":"uid-1","email":"ada@example.com","displayName":"Ada Lovelace",
                     "photoUrl":"https://example.com/ada.png","tenantId":"campo-de-una-version-futura"}
                    """.trimIndent(),
                )
            val store = KeychainTokenStore(storage)

            // When / Then
            assertEquals(session(), store.load())
        }

    private fun session(
        uid: String = "uid-1",
        email: String? = "ada@example.com",
        displayName: String? = "Ada Lovelace",
        photoUrl: String? = "https://example.com/ada.png",
    ) = StoredSession(
        idToken = "un-token",
        refreshToken = "un-refresh",
        expiresAtEpochSeconds = 1767225600L,
        uid = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl,
    )
}
