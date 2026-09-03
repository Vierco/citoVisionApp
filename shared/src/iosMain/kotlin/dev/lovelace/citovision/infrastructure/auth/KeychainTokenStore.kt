package dev.lovelace.citovision.infrastructure.auth

import io.github.aakira.napier.Napier
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * [TokenStore] de iOS sobre el **Keychain**, tal y como exige SECURITY_MOBILE §Tokens ("iOS: Keychain")
 * y para que la sesión sobreviva a los reinicios (SPEC-0001 RF-8, ADR-0006).
 *
 * Esta clase solo se ocupa de **serializar**: el acceso al Keychain vive detrás de [KeychainStorage],
 * que además es lo que hace testeable la decisión de descartar una sesión ilegible.
 *
 * Ningún valor de token se escribe en logs (RNF-5, AGENTS.md §11).
 */
class KeychainTokenStore internal constructor(
    private val storage: KeychainStorage,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : TokenStore {
    /** Constructor de producción: el ítem real del Keychain de la app. */
    constructor() : this(SecItemKeychainStorage())

    override suspend fun load(): StoredSession? {
        val stored = storage.read() ?: return null
        return try {
            json.decodeFromString(StoredSession.serializer(), stored)
        } catch (e: SerializationException) {
            // Formato antiguo o dato corrupto: se descarta para forzar un login limpio.
            Napier.w("Sesión guardada ilegible; se descarta", e, tag = "Auth")
            storage.delete()
            null
        }
    }

    override suspend fun save(session: StoredSession) {
        // Serializador explícito: la sobrecarga reificada de `encodeToString` exige un import extra y aquí
        // no aporta nada.
        storage.write(json.encodeToString(StoredSession.serializer(), session))
    }

    override suspend fun clear() {
        storage.delete()
    }
}
