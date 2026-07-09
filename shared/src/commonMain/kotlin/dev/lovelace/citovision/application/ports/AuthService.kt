package dev.lovelace.citovision.application.ports

import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.AuthUser
import dev.lovelace.citovision.domain.errors.AuthError
import kotlinx.coroutines.flow.Flow

/**
 * Puerto de autenticación (ver SPEC-0001). Implementado por plataforma:
 * Firebase en androidMain; stub en commonMain (usado por Desktop e iOS en fase 1).
 * El `idToken` de Google lo resuelve el flujo nativo de cada plataforma antes de llamar aquí.
 */
interface AuthService {
    val currentUser: Flow<AuthUser?>

    suspend fun signInWithEmail(
        email: String,
        password: String,
    ): Result<AuthUser, AuthError>

    suspend fun signInWithGoogle(idToken: String): Result<AuthUser, AuthError>

    suspend fun signInAsGuest(): Result<AuthUser, AuthError>

    suspend fun signOut(): Result<Unit, AuthError>

    /** Envía un correo de restablecimiento de contraseña (SPEC-0002). */
    suspend fun sendPasswordReset(email: String): Result<Unit, AuthError>
}
