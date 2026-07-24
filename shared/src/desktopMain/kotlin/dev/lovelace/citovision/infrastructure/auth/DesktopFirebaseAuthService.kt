package dev.lovelace.citovision.infrastructure.auth

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.AuthTokenProvider
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.AuthUser
import dev.lovelace.citovision.domain.errors.AuthError
import dev.lovelace.citovision.infrastructure.auth.remote.IdentityToolkitAuthDataSource
import dev.lovelace.citovision.infrastructure.auth.remote.RefreshTokenResponse
import dev.lovelace.citovision.infrastructure.auth.remote.SignInResponse
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Implementación de [AuthService] para Desktop contra Firebase Auth REST (Identity Toolkit), ADR-0002.
 * La sesión de cuenta vive SOLO en memoria: al reiniciar la app se requiere re-login (desviación
 * consciente de RF-8, únicamente Desktop). Google no está soportado en Desktop en esta fase; el acceso
 * como invitado es local, igual que en el resto de plataformas.
 *
 * Es además el [AuthTokenProvider] de Desktop: guarda el `idToken`/`refreshToken` del login y lo renueva
 * contra Secure Token cuando está a punto de caducar, para autorizar las llamadas a Firestore y Storage.
 * Los tokens nunca se persisten ni se loguean (SECURITY_MOBILE §Tokens, AGENTS.md §11).
 */
class DesktopFirebaseAuthService(
    private val remote: IdentityToolkitAuthDataSource,
    private val clock: Clock = Clock.System,
) : AuthService,
    AuthTokenProvider {
    private val currentUserState = MutableStateFlow<AuthUser?>(null)
    override val currentUser: Flow<AuthUser?> = currentUserState.asStateFlow()

    /** Protege la sesión de tokens: varias llamadas remotas concurrentes no deben refrescar a la vez. */
    private val tokenMutex = Mutex()
    private var tokens: TokenSession? = null

    override suspend fun signInWithEmail(
        email: String,
        password: String,
    ): Result<AuthUser, AuthError> =
        when (val result = remote.signInWithPassword(email, password)) {
            is Result.Success -> {
                val user = result.value.toAuthUser()
                tokenMutex.withLock { tokens = result.value.toTokenSession() }
                currentUserState.update { user }
                Result.Success(user)
            }
            is Result.Failure -> result
        }

    override suspend fun signInWithGoogle(idToken: String): Result<AuthUser, AuthError> =
        Result.Failure(AuthError.NotSupportedOnPlatform)

    override suspend fun signInAsGuest(): Result<AuthUser, AuthError> {
        val guest = AuthUser(uid = GUEST_UID, email = null, displayName = null, isGuest = true)
        tokenMutex.withLock { tokens = null }
        currentUserState.update { guest }
        return Result.Success(guest)
    }

    override suspend fun signOut(): Result<Unit, AuthError> {
        tokenMutex.withLock { tokens = null }
        currentUserState.update { null }
        return Result.Success(Unit)
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit, AuthError> =
        remote.sendPasswordResetEmail(email)

    /**
     * ID token vigente de la sesión de cuenta, renovándolo si le queda menos de [REFRESH_MARGIN]. Sin
     * sesión (invitado, sin login o refresh rechazado) devuelve `null`.
     */
    override suspend fun currentIdToken(): String? =
        tokenMutex.withLock {
            val current = tokens ?: return@withLock null
            if (clock.now() < current.expiresAt - REFRESH_MARGIN) current.idToken else refresh(current)
        }

    /** Llamado siempre con [tokenMutex] tomado. */
    private suspend fun refresh(current: TokenSession): String? =
        when (val result = remote.refreshIdToken(current.refreshToken)) {
            is Result.Success -> {
                val renewed = result.value.toTokenSession()
                tokens = renewed
                renewed.idToken
            }
            is Result.Failure -> {
                // El refresh token ya no sirve (caducado o revocado): la sesión de cuenta termina aquí y
                // la UI volverá a pedir login. No se registra ningún valor de token.
                Napier.w("No se pudo renovar el ID token; se requiere iniciar sesión de nuevo", tag = "Auth")
                tokens = null
                currentUserState.update { null }
                null
            }
        }

    private fun SignInResponse.toAuthUser(): AuthUser =
        AuthUser(uid = localId, email = email, displayName = displayName, isGuest = false)

    private fun SignInResponse.toTokenSession(): TokenSession =
        TokenSession(idToken = idToken, refreshToken = refreshToken, expiresAt = expiresAtFrom(expiresIn))

    private fun RefreshTokenResponse.toTokenSession(): TokenSession =
        TokenSession(idToken = idToken, refreshToken = refreshToken, expiresAt = expiresAtFrom(expiresIn))

    private fun expiresAtFrom(expiresIn: String): Instant =
        clock.now() + (expiresIn.toLongOrNull() ?: DEFAULT_EXPIRES_IN_SECONDS).seconds

    /** Credenciales de la sesión activa. Solo en memoria y solo dentro de esta clase. */
    private data class TokenSession(
        val idToken: String,
        val refreshToken: String,
        val expiresAt: Instant,
    )

    private companion object {
        const val GUEST_UID = "guest"
        const val DEFAULT_EXPIRES_IN_SECONDS = 3600L
        val REFRESH_MARGIN = 5.minutes
    }
}
