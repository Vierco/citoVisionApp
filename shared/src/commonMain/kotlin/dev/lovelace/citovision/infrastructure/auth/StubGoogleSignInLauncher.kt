package dev.lovelace.citovision.infrastructure.auth

import dev.lovelace.citovision.application.ports.GoogleSignInLauncher
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.errors.AuthError

/**
 * Stub de [GoogleSignInLauncher] para plataformas sin flujo nativo en fase 1 (Desktop, iOS).
 * Devuelve [AuthError.NotSupportedOnPlatform], coherente con SPEC-0001 (CA-6).
 */
class StubGoogleSignInLauncher : GoogleSignInLauncher {
    override suspend fun requestIdToken(): Result<String, AuthError> = Result.Failure(AuthError.NotSupportedOnPlatform)
}
