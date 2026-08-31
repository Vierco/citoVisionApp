package dev.lovelace.citovision.infrastructure.auth.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs de la API REST de Firebase Auth (Identity Toolkit) y del endpoint de refresco de token
 * (Secure Token). Solo existen en Infrastructure (RULES.md); no cruzan a Application/Domain.
 */

@Serializable
data class SignInRequest(
    val email: String,
    val password: String,
    val returnSecureToken: Boolean = true,
)

/**
 * Respuesta común de los endpoints de login (`signInWithPassword` y `signInWithIdp`). El segundo
 * devuelve además campos del proveedor federado que aquí no se usan; el cliente ignora las claves
 * desconocidas.
 */
@Serializable
data class SignInResponse(
    val idToken: String,
    val refreshToken: String,
    val expiresIn: String,
    val localId: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
)

/**
 * Petición de `accounts:signInWithIdp`: canjea el ID token de un proveedor federado (Google) por una
 * sesión de Firebase (ADR-0006). Las credenciales del proveedor viajan dentro de [postBody] con
 * formato de query string, tal y como exige Identity Toolkit.
 *
 * [requestUri] es obligatorio para el endpoint pero irrelevante en un flujo nativo (no hay redirección
 * web que validar), de ahí el valor local por defecto.
 */
@Serializable
data class SignInWithIdpRequest(
    val postBody: String,
    val requestUri: String = DEFAULT_REQUEST_URI,
    val returnSecureToken: Boolean = true,
)

private const val DEFAULT_REQUEST_URI = "http://localhost"

@Serializable
data class SendOobCodeRequest(
    val requestType: String,
    val email: String,
)

/** Respuesta de `securetoken.googleapis.com` — usa snake_case. */
@Serializable
data class RefreshTokenResponse(
    @SerialName("id_token") val idToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: String,
    @SerialName("user_id") val userId: String,
)

@Serializable
data class IdentityToolkitErrorResponse(
    val error: IdentityToolkitError,
)

@Serializable
data class IdentityToolkitError(
    val code: Int,
    val message: String,
)
