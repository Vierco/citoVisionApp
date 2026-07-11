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

@Serializable
data class SignInResponse(
    val idToken: String,
    val refreshToken: String,
    val expiresIn: String,
    val localId: String,
    val email: String? = null,
    val displayName: String? = null,
)

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
