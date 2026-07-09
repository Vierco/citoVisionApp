package dev.lovelace.citovision.domain.errors

/**
 * Errores de autenticación de dominio (ver SPEC-0001). Cerrado: cada caso es un estado conocido.
 * Infrastructure traduce las excepciones técnicas de Firebase a estos casos (RNF-4).
 */
sealed interface AuthError {
    data object InvalidCredentials : AuthError

    data object UserNotFound : AuthError

    data object Network : AuthError

    data object TooManyRequests : AuthError

    data object GoogleSignInCancelled : AuthError

    data object GoogleSignInFailed : AuthError

    data object NotSupportedOnPlatform : AuthError

    data class Unknown(
        val cause: String?,
    ) : AuthError
}
