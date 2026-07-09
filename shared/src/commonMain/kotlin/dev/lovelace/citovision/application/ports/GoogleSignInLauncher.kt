package dev.lovelace.citovision.application.ports

import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.errors.AuthError

/**
 * Puerto que lanza el flujo nativo de Google Sign-In y devuelve el `idToken` de la cuenta elegida.
 *
 * La obtención de la credencial es específica de plataforma (Android: Credential Manager; Desktop:
 * OAuth web en fase 2), por eso vive tras una abstracción (ver SPEC-0001, nota de plataforma).
 * El `idToken` resultante se entrega a [AuthService.signInWithGoogle].
 */
interface GoogleSignInLauncher {
    suspend fun requestIdToken(): Result<String, AuthError>
}
