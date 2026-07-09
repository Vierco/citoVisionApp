package dev.lovelace.citovision.domain.entities

/**
 * Usuario autenticado, modelo de dominio. No contiene tipos de Firebase (ver SPEC-0001, RNF-4).
 * `isGuest = true` representa una sesión local de invitado (sin cuenta ni backend).
 */
data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val isGuest: Boolean,
)
