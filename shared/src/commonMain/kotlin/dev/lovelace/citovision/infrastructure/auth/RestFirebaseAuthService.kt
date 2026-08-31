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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Implementación de [AuthService] contra Firebase Auth REST (Identity Toolkit), compartida por Desktop
 * (ADR-0002) e iOS (ADR-0006). No depende de ningún SDK nativo de Firebase, así que no impone nada al
 * enlazado del framework de iOS.
 *
 * Es además el [AuthTokenProvider] de esas plataformas: conserva el `idToken`/`refreshToken` del login y
 * lo renueva contra Secure Token cuando está a punto de caducar, para autorizar Firestore y Storage
 * (ADR-0001).
 *
 * Dónde sobreviven esos tokens lo decide el [TokenStore] inyectado: Keychain en iOS (la sesión persiste
 * entre reinicios, SPEC-0001 RF-8) y memoria en Desktop (re-login al arrancar, desviación consciente de
 * ADR-0002). Los tokens nunca se loguean (RNF-5, AGENTS.md §11).
 *
 * El acceso como invitado es local y no crea cuenta (SPEC-0001 RN-3).
 */
class RestFirebaseAuthService(
    private val remote: IdentityToolkitAuthDataSource,
    private val tokenStore: TokenStore,
    private val clock: Clock = Clock.System,
) : AuthService,
    AuthTokenProvider {
    private val currentUserState = MutableStateFlow<AuthUser?>(null)

    /**
     * Restaura la sesión guardada antes de exponer el estado, para que la Splash vea al usuario que ya
     * había iniciado sesión (RF-8) sin que nadie tenga que invocar un `init` aparte.
     */
    override val currentUser: Flow<AuthUser?> =
        flow {
            ensureRestored()
            emitAll(currentUserState)
        }

    /** Protege la sesión de tokens: varias llamadas remotas concurrentes no deben refrescar a la vez. */
    private val tokenMutex = Mutex()
    private var tokens: TokenSession? = null
    private var restored = false

    override suspend fun signInWithEmail(
        email: String,
        password: String,
    ): Result<AuthUser, AuthError> = startSessionFrom(remote.signInWithPassword(email, password))

    override suspend fun signInWithGoogle(idToken: String): Result<AuthUser, AuthError> =
        startSessionFrom(remote.signInWithGoogleIdToken(idToken))

    override suspend fun signInAsGuest(): Result<AuthUser, AuthError> {
        val guest = AuthUser(uid = GUEST_UID, email = null, displayName = null, isGuest = true)
        endSession()
        currentUserState.update { guest }
        return Result.Success(guest)
    }

    override suspend fun signOut(): Result<Unit, AuthError> {
        endSession()
        currentUserState.update { null }
        return Result.Success(Unit)
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit, AuthError> =
        remote.sendPasswordResetEmail(email)

    /**
     * ID token vigente de la sesión de cuenta, renovándolo si le queda menos de [REFRESH_MARGIN]. Sin
     * sesión (invitado, sin login o refresh rechazado) devuelve `null`.
     */
    override suspend fun currentIdToken(): String? {
        // Fuera del mutex: `ensureRestored` lo toma y `Mutex` no es reentrante.
        ensureRestored()
        return tokenMutex.withLock {
            val current = tokens ?: return@withLock null
            if (clock.now() < current.expiresAt - REFRESH_MARGIN) current.idToken else refresh(current)
        }
    }

    private suspend fun startSessionFrom(result: Result<SignInResponse, AuthError>): Result<AuthUser, AuthError> =
        when (result) {
            is Result.Success -> {
                val user = result.value.toAuthUser()
                val session = result.value.toTokenSession()
                tokenMutex.withLock {
                    // Ya hay sesión viva: una restauración posterior no debe pisarla.
                    restored = true
                    tokens = session
                    tokenStore.save(result.value.toStoredSession(session.expiresAt))
                }
                currentUserState.update { user }
                Result.Success(user)
            }
            is Result.Failure -> result
        }

    /** Borra toda credencial, en memoria y en el almacén seguro (RF-6). */
    private suspend fun endSession() {
        tokenMutex.withLock {
            restored = true
            tokens = null
            tokenStore.clear()
        }
    }

    /** Carga una sola vez la sesión persistida. Toma [tokenMutex]: no llamar con el lock ya tomado. */
    private suspend fun ensureRestored() {
        tokenMutex.withLock {
            if (restored) return@withLock
            restored = true
            val stored = tokenStore.load() ?: return@withLock
            tokens = stored.toTokenSession()
            currentUserState.update { stored.toAuthUser() }
        }
    }

    /** Llamado siempre con [tokenMutex] tomado. */
    private suspend fun refresh(current: TokenSession): String? =
        when (val result = remote.refreshIdToken(current.refreshToken)) {
            is Result.Success -> {
                val renewed = result.value.toTokenSession()
                tokens = renewed
                currentUserState.value?.let { user ->
                    tokenStore.save(renewed.toStoredSession(user))
                }
                renewed.idToken
            }
            is Result.Failure -> {
                // El refresh token ya no sirve (caducado o revocado): la sesión de cuenta termina aquí y
                // la UI volverá a pedir login. No se registra ningún valor de token.
                Napier.w("No se pudo renovar el ID token; se requiere iniciar sesión de nuevo", tag = "Auth")
                tokens = null
                tokenStore.clear()
                currentUserState.update { null }
                null
            }
        }

    private fun SignInResponse.toAuthUser(): AuthUser =
        AuthUser(
            uid = localId,
            email = email,
            displayName = displayName,
            isGuest = false,
            photoUrl = photoUrl,
        )

    private fun SignInResponse.toStoredSession(expiresAt: Instant): StoredSession =
        StoredSession(
            idToken = idToken,
            refreshToken = refreshToken,
            expiresAtEpochSeconds = expiresAt.epochSeconds,
            uid = localId,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
        )

    private fun TokenSession.toStoredSession(user: AuthUser): StoredSession =
        StoredSession(
            idToken = idToken,
            refreshToken = refreshToken,
            expiresAtEpochSeconds = expiresAt.epochSeconds,
            uid = user.uid,
            email = user.email,
            displayName = user.displayName,
            photoUrl = user.photoUrl,
        )

    private fun StoredSession.toAuthUser(): AuthUser =
        AuthUser(
            uid = uid,
            email = email,
            displayName = displayName,
            isGuest = false,
            photoUrl = photoUrl,
        )

    private fun StoredSession.toTokenSession(): TokenSession =
        TokenSession(
            idToken = idToken,
            refreshToken = refreshToken,
            expiresAt = Instant.fromEpochSeconds(expiresAtEpochSeconds),
        )

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
