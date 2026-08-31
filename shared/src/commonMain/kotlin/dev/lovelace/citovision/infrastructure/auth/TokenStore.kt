package dev.lovelace.citovision.infrastructure.auth

import kotlinx.serialization.Serializable

/**
 * Sesión de cuenta persistida (ADR-0006). Además de las credenciales guarda la identidad mínima del
 * usuario, para poder restaurar la sesión al arrancar (SPEC-0001 RF-8) sin una llamada extra al
 * servidor.
 *
 * `expiresAt` se guarda como segundos epoch para que el formato persistido no dependa de tipos de
 * Kotlin. Nunca se escribe en logs (RNF-5, AGENTS.md §11).
 */
@Serializable
data class StoredSession(
    val idToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
    val uid: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
)

/**
 * Almacén de la sesión de cuenta. Es un colaborador **interno de Infrastructure**: Application solo
 * conoce `AuthService`/`AuthTokenProvider`, y cómo se guardan los tokens es un detalle de esta capa.
 *
 * Cada plataforma aporta su implementación vía Koin, según lo que exige SECURITY_MOBILE §Tokens:
 * Keychain en iOS y memoria en Desktop (ADR-0002; ahí la sesión no sobrevive al reinicio).
 */
interface TokenStore {
    suspend fun load(): StoredSession?

    suspend fun save(session: StoredSession)

    suspend fun clear()
}
