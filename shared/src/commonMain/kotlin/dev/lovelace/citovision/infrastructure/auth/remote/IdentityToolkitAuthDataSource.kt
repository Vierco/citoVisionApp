package dev.lovelace.citovision.infrastructure.auth.remote

import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.errors.AuthError
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.parameters
import io.ktor.serialization.ContentConvertException
import kotlin.coroutines.cancellation.CancellationException

/**
 * DataSource remoto de autenticación contra la API REST de Firebase Auth (Identity Toolkit), consumido
 * por la implementación Desktop de `AuthService` (ADR-0002). Traduce los códigos de error de Identity
 * Toolkit a [AuthError] (RNF-4 de SPEC-0001); los DTO no salen de Infrastructure.
 *
 * La [apiKey] es la Web API key del proyecto Firebase (identificador público, no secreto), inyectada
 * desde build config; nunca se hardcodea.
 */
class IdentityToolkitAuthDataSource(
    private val client: HttpClient,
    private val apiKey: String,
) {
    suspend fun signInWithPassword(
        email: String,
        password: String,
    ): Result<SignInResponse, AuthError> =
        runCatchingRest(::mapSignInErrorCode) {
            val response =
                client
                    .post("${IDENTITY_TOOLKIT_BASE}accounts:signInWithPassword") {
                        parameter("key", apiKey)
                        contentType(ContentType.Application.Json)
                        setBody(SignInRequest(email = email, password = password))
                    }.body<SignInResponse>()
            Result.Success(response)
        }

    /**
     * Canjea el ID token de Google por una sesión de Firebase (ADR-0006, SPEC-0001 RF-2). El `idToken`
     * lo obtiene el flujo nativo de cada plataforma a través de `GoogleSignInLauncher`; aquí solo se
     * intercambia. Nunca se loguea su valor.
     */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<SignInResponse, AuthError> =
        runCatchingRest(::mapIdpErrorCode) {
            val response =
                client
                    .post("${IDENTITY_TOOLKIT_BASE}accounts:signInWithIdp") {
                        parameter("key", apiKey)
                        contentType(ContentType.Application.Json)
                        setBody(SignInWithIdpRequest(postBody = "id_token=$idToken&providerId=$GOOGLE_PROVIDER_ID"))
                    }.body<SignInResponse>()
            Result.Success(response)
        }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit, AuthError> =
        runCatchingRest(::mapResetErrorCode) {
            client.post("${IDENTITY_TOOLKIT_BASE}accounts:sendOobCode") {
                parameter("key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(SendOobCodeRequest(requestType = PASSWORD_RESET_REQUEST_TYPE, email = email))
            }
            Result.Success(Unit)
        }

    suspend fun refreshIdToken(refreshToken: String): Result<RefreshTokenResponse, AuthError> =
        runCatchingRest(::mapRefreshErrorCode) {
            val response =
                client
                    .submitForm(
                        url = SECURE_TOKEN_URL,
                        formParameters =
                            parameters {
                                append("grant_type", "refresh_token")
                                append("refresh_token", refreshToken)
                            },
                    ) {
                        parameter("key", apiKey)
                    }.body<RefreshTokenResponse>()
            Result.Success(response)
        }

    private suspend fun <T> runCatchingRest(
        mapErrorCode: (String?) -> AuthError,
        block: suspend () -> Result<T, AuthError>,
    ): Result<T, AuthError> =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: ClientRequestException) {
            Result.Failure(mapErrorCode(e.identityToolkitErrorCode()))
        } catch (e: ServerResponseException) {
            Result.Failure(AuthError.Unknown("HTTP ${e.response.status.value}"))
        } catch (e: HttpRequestTimeoutException) {
            Result.Failure(AuthError.Network)
        } catch (e: ContentConvertException) {
            // Respuesta ilegible / contrato inesperado: NO es un fallo de red.
            Napier.w("Respuesta de auth REST ilegible", e, tag = "IdentityToolkit")
            Result.Failure(AuthError.Unknown("serialization"))
        } catch (e: Exception) {
            // Boundary de red del RemoteDataSource: sin conexión o socket.
            Napier.w("Fallo no tipado en auth REST", e, tag = "IdentityToolkit")
            Result.Failure(AuthError.Network)
        }

    private suspend fun ClientRequestException.identityToolkitErrorCode(): String? =
        try {
            response.body<IdentityToolkitErrorResponse>().error.message
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("No se pudo parsear el error de Identity Toolkit", e, tag = "IdentityToolkit")
            null
        }

    private fun mapSignInErrorCode(code: String?): AuthError =
        when {
            code == null -> AuthError.Unknown(null)
            code.startsWith(TOO_MANY_ATTEMPTS_PREFIX) -> AuthError.TooManyRequests
            code in SIGN_IN_INVALID_CREDENTIAL_CODES -> AuthError.InvalidCredentials
            else -> AuthError.Unknown(code)
        }

    private fun mapResetErrorCode(code: String?): AuthError =
        when {
            code == null -> AuthError.Unknown(null)
            code == EMAIL_NOT_FOUND -> AuthError.UserNotFound
            code.startsWith(
                TOO_MANY_ATTEMPTS_PREFIX,
            ) ||
                code == RESET_PASSWORD_EXCEED_LIMIT -> AuthError.TooManyRequests
            else -> AuthError.Unknown(code)
        }

    /**
     * Un fallo del canje federado no es culpa de unas credenciales mal escritas por el usuario: la
     * credencial de Google ya era válida cuando llegó aquí. Se traduce a [AuthError.GoogleSignInFailed]
     * salvo los casos genéricos de cuenta.
     */
    private fun mapIdpErrorCode(code: String?): AuthError =
        when {
            code == null -> AuthError.GoogleSignInFailed
            code.startsWith(TOO_MANY_ATTEMPTS_PREFIX) -> AuthError.TooManyRequests
            code in IDP_USER_CODES -> AuthError.UserNotFound
            else -> AuthError.GoogleSignInFailed
        }

    private fun mapRefreshErrorCode(code: String?): AuthError =
        when {
            code == null -> AuthError.Unknown(null)
            code in REFRESH_REAUTH_CODES -> AuthError.InvalidCredentials
            else -> AuthError.Unknown(code)
        }

    private companion object {
        const val IDENTITY_TOOLKIT_BASE = "https://identitytoolkit.googleapis.com/v1/"
        const val SECURE_TOKEN_URL = "https://securetoken.googleapis.com/v1/token"
        const val PASSWORD_RESET_REQUEST_TYPE = "PASSWORD_RESET"
        const val GOOGLE_PROVIDER_ID = "google.com"
        const val TOO_MANY_ATTEMPTS_PREFIX = "TOO_MANY_ATTEMPTS_TRY_LATER"
        const val RESET_PASSWORD_EXCEED_LIMIT = "RESET_PASSWORD_EXCEED_LIMIT"
        const val EMAIL_NOT_FOUND = "EMAIL_NOT_FOUND"

        val SIGN_IN_INVALID_CREDENTIAL_CODES =
            setOf(
                "EMAIL_NOT_FOUND",
                "INVALID_PASSWORD",
                "INVALID_LOGIN_CREDENTIALS",
                "INVALID_EMAIL",
                "MISSING_PASSWORD",
                "USER_DISABLED",
            )

        val IDP_USER_CODES =
            setOf(
                "USER_DISABLED",
                "USER_NOT_FOUND",
                "OPERATION_NOT_ALLOWED",
            )

        val REFRESH_REAUTH_CODES =
            setOf(
                "TOKEN_EXPIRED",
                "INVALID_REFRESH_TOKEN",
                "USER_NOT_FOUND",
                "USER_DISABLED",
            )
    }
}
