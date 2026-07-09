package dev.lovelace.citovision.infrastructure.auth

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.AuthUser
import dev.lovelace.citovision.domain.errors.AuthError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Implementación de auth para plataformas sin Firebase (Desktop e iOS en fase 1).
 * El login con cuenta (email/Google) no está soportado y devuelve [AuthError.NotSupportedOnPlatform];
 * el acceso como invitado funciona de forma local (SPEC-0001, RN-3). Fase 2: Desktop real.
 */
class StubAuthService : AuthService {
    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    override val currentUser: Flow<AuthUser?> = _currentUser.asStateFlow()

    override suspend fun signInWithEmail(
        email: String,
        password: String,
    ): Result<AuthUser, AuthError> = Result.Failure(AuthError.NotSupportedOnPlatform)

    override suspend fun signInWithGoogle(idToken: String): Result<AuthUser, AuthError> =
        Result.Failure(AuthError.NotSupportedOnPlatform)

    override suspend fun signInAsGuest(): Result<AuthUser, AuthError> {
        val guest = AuthUser(uid = GUEST_UID, email = null, displayName = null, isGuest = true)
        _currentUser.update { guest }
        return Result.Success(guest)
    }

    override suspend fun signOut(): Result<Unit, AuthError> {
        _currentUser.update { null }
        return Result.Success(Unit)
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit, AuthError> =
        Result.Failure(AuthError.NotSupportedOnPlatform)

    private companion object {
        const val GUEST_UID = "guest"
    }
}
