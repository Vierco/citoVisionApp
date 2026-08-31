package dev.lovelace.citovision.infrastructure.auth

import dev.lovelace.citovision.application.ports.GoogleSignInLauncher
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.errors.AuthError
import io.github.aakira.napier.Napier
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * [GoogleSignInLauncher] de iOS (ADR-0006, SPEC-0001 RF-2). No habla con el SDK de Google: delega en el
 * lanzador que Swift instala en [GoogleSignInBridge] y convierte su respuesta en un [Result] de dominio.
 *
 * El `idToken` resultante lo canjea después `AuthService.signInWithGoogle` por una sesión de Firebase.
 * Su valor nunca se escribe en logs (RNF-5).
 */
class IosGoogleSignInLauncher : GoogleSignInLauncher {
    override suspend fun requestIdToken(): Result<String, AuthError> =
        suspendCancellableCoroutine { continuation ->
            val presenter = GoogleSignInBridge.presenter
            if (presenter == null) {
                // La app se ha construido sin el puente Swift: es un error de integración, no del usuario.
                Napier.w("Google Sign-In no está instalado desde Swift", tag = "Auth")
                continuation.resume(Result.Failure(AuthError.GoogleSignInFailed))
                return@suspendCancellableCoroutine
            }
            presenter { idToken, error ->
                // El callback nativo podría llegar tarde (pantalla ya descartada): solo se reanuda una vez.
                if (continuation.isActive) {
                    continuation.resume(toResult(idToken, error))
                }
            }
        }

    private fun toResult(
        idToken: String?,
        error: String?,
    ): Result<String, AuthError> =
        when {
            error == GoogleSignInBridge.CANCELLED -> Result.Failure(AuthError.GoogleSignInCancelled)
            error != null -> {
                Napier.w("Google Sign-In falló en el flujo nativo: $error", tag = "Auth")
                Result.Failure(AuthError.GoogleSignInFailed)
            }
            idToken.isNullOrBlank() -> Result.Failure(AuthError.GoogleSignInFailed)
            else -> Result.Success(idToken)
        }
}
