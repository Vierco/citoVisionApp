package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.GoogleSignInLauncher
import dev.lovelace.citovision.application.ports.SessionRepository
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.core.result.fold
import dev.lovelace.citovision.domain.entities.AuthUser
import dev.lovelace.citovision.domain.errors.AuthError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * Casos de uso de autenticación. El ViewModel invoca estos use cases, nunca el puerto directamente
 * (ver Skill mvvm-compose-kmp). La validación de formato de campos vive en Presentation.
 */
class SignInWithEmailUseCase(
    private val authService: AuthService,
) {
    suspend operator fun invoke(
        email: String,
        password: String,
    ): Result<AuthUser, AuthError> = authService.signInWithEmail(email, password)
}

/**
 * Orquesta el login con Google: resuelve el `idToken` con el flujo nativo ([GoogleSignInLauncher])
 * y lo canjea en Firebase ([AuthService.signInWithGoogle]). Un fallo del flujo nativo (cancelado,
 * no soportado) se propaga tal cual sin llamar al backend.
 */
class SignInWithGoogleUseCase(
    private val authService: AuthService,
    private val googleSignInLauncher: GoogleSignInLauncher,
) {
    suspend operator fun invoke(): Result<AuthUser, AuthError> =
        googleSignInLauncher.requestIdToken().fold(
            onSuccess = { idToken -> authService.signInWithGoogle(idToken) },
            onFailure = { error -> Result.Failure(error) },
        )
}

/**
 * Inicia una sesión de invitado (local) y persiste el flag para que sobreviva a reinicios
 * (SPEC-0001 RF-8, RN-4). El flag solo se marca si el inicio de sesión tiene éxito.
 */
class SignInAsGuestUseCase(
    private val authService: AuthService,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(): Result<AuthUser, AuthError> =
        authService.signInAsGuest().also { result ->
            if (result is Result.Success) sessionRepository.setGuestSession(true)
        }
}

/**
 * Cierra la sesión: limpia el flag de invitado persistido y cierra la sesión de cuenta en Firebase
 * (SPEC-0001 RF-6). Tras esto, el siguiente arranque lleva a `LoginRoute` (CA-9).
 */
class SignOutUseCase(
    private val authService: AuthService,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(): Result<Unit, AuthError> {
        sessionRepository.setGuestSession(false)
        return authService.signOut()
    }
}

/**
 * Determina si hay una sesión activa al arrancar (SPEC-0001 RF-8): cuenta Firebase restaurada por su
 * SDK, o sesión de invitado persistida. Lo consume la Splash para decidir `MainRoute` vs `LoginRoute`.
 */
class HasActiveSessionUseCase(
    private val authService: AuthService,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(): Boolean {
        val hasAccount = authService.currentUser.first() != null
        return hasAccount || sessionRepository.isGuestSession().first()
    }
}

/** Tipo de sesión activa, para decidir qué opciones mostrar en Ajustes. */
enum class SessionStatus { ACCOUNT, GUEST, NONE }

/**
 * Observa el tipo de sesión combinando la cuenta Firebase y el flag de invitado. Robusto entre
 * plataformas: en Desktop el invitado aparece como `currentUser` con `isGuest=true`; en Android el
 * invitado es local (currentUser null) y solo consta en el flag persistido.
 */
class ObserveSessionStatusUseCase(
    private val authService: AuthService,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(): Flow<SessionStatus> =
        combine(authService.currentUser, sessionRepository.isGuestSession()) { user, isGuestFlag ->
            when {
                user != null && !user.isGuest -> SessionStatus.ACCOUNT
                user?.isGuest == true || isGuestFlag -> SessionStatus.GUEST
                else -> SessionStatus.NONE
            }
        }
}

class SendPasswordResetUseCase(
    private val authService: AuthService,
) {
    suspend operator fun invoke(email: String): Result<Unit, AuthError> = authService.sendPasswordReset(email)
}
