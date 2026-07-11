package dev.lovelace.citovision.infrastructure.auth

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.AuthUser
import dev.lovelace.citovision.domain.errors.AuthError
import dev.lovelace.citovision.infrastructure.auth.remote.IdentityToolkitAuthDataSource
import dev.lovelace.citovision.infrastructure.auth.remote.SignInResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Implementación de [AuthService] para Desktop contra Firebase Auth REST (Identity Toolkit), ADR-0002.
 * La sesión de cuenta vive SOLO en memoria: al reiniciar la app se requiere re-login (desviación
 * consciente de RF-8, únicamente Desktop). Google no está soportado en Desktop en esta fase; el acceso
 * como invitado es local, igual que en el resto de plataformas.
 */
class DesktopFirebaseAuthService(
    private val remote: IdentityToolkitAuthDataSource,
) : AuthService {
    private val currentUserState = MutableStateFlow<AuthUser?>(null)
    override val currentUser: Flow<AuthUser?> = currentUserState.asStateFlow()

    override suspend fun signInWithEmail(
        email: String,
        password: String,
    ): Result<AuthUser, AuthError> =
        when (val result = remote.signInWithPassword(email, password)) {
            is Result.Success -> {
                val user = result.value.toAuthUser()
                currentUserState.update { user }
                Result.Success(user)
            }
            is Result.Failure -> result
        }

    override suspend fun signInWithGoogle(idToken: String): Result<AuthUser, AuthError> =
        Result.Failure(AuthError.NotSupportedOnPlatform)

    override suspend fun signInAsGuest(): Result<AuthUser, AuthError> {
        val guest = AuthUser(uid = GUEST_UID, email = null, displayName = null, isGuest = true)
        currentUserState.update { guest }
        return Result.Success(guest)
    }

    override suspend fun signOut(): Result<Unit, AuthError> {
        currentUserState.update { null }
        return Result.Success(Unit)
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit, AuthError> =
        remote.sendPasswordResetEmail(email)

    private fun SignInResponse.toAuthUser(): AuthUser =
        AuthUser(uid = localId, email = email, displayName = displayName, isGuest = false)

    private companion object {
        const val GUEST_UID = "guest"
    }
}
