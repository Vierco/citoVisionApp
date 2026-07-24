package dev.lovelace.citovision.infrastructure.network

import dev.lovelace.citovision.application.ports.AuthTokenProvider
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.bearerAuth
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Clientes HTTP del proyecto (RULES.md §Networking). El engine es lo único específico de plataforma y
 * se inyecta desde el `platformModule` (OkHttp en Android/Desktop, Darwin en iOS); `commonMain` nunca
 * importa un engine concreto. Ambos clientes comparten configuración y engine, y se diferencian solo en
 * si adjuntan el ID token del usuario:
 *
 * - [createHttpClient]: sin credenciales. Lo usa Identity Toolkit, donde **no debe** viajar el token
 *   (el login es precisamente quien lo emite).
 * - [createAuthorizedHttpClient]: añade `Authorization: Bearer <ID token>`. Lo usan Firestore y Storage,
 *   cuyas reglas exigen `request.auth` (ADR-0001).
 *
 * Logging enrutado a Napier (RULES.md §Logging: prohibido `println`/`Log.*`). Nivel `HEADERS`: se
 * registran línea de petición y cabeceras, **nunca el cuerpo**, de modo que contraseñas y tokens que
 * viajan en el body no se loguean; además la cabecera `Authorization` se sanea explícitamente.
 */
fun createHttpClient(engine: HttpClientEngine): HttpClient =
    HttpClient(engine) {
        applyDefaults()
    }

fun createAuthorizedHttpClient(
    engine: HttpClientEngine,
    tokenProvider: AuthTokenProvider,
): HttpClient =
    HttpClient(engine) {
        applyDefaults()
        install(firebaseIdTokenPlugin(tokenProvider))
    }

/**
 * Adjunta el ID token vigente a cada petición. Si no hay sesión de cuenta (invitado, sin login o token
 * irrecuperable) la petición sale sin cabecera y el servidor la rechazará: el cliente no decide el
 * acceso, solo se identifica (SECURITY_MOBILE §Autenticación).
 */
private fun firebaseIdTokenPlugin(tokenProvider: AuthTokenProvider) =
    createClientPlugin(FIREBASE_ID_TOKEN_PLUGIN) {
        onRequest { request, _ ->
            tokenProvider.currentIdToken()?.let { token -> request.bearerAuth(token) }
        }
    }

private fun HttpClientConfig<*>.applyDefaults() {
    expectSuccess = true
    install(Logging) {
        logger = NapierKtorLogger
        level = LogLevel.HEADERS
        sanitizeHeader { header -> header == HttpHeaders.Authorization }
    }
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                // Codifica valores por defecto en el body (p. ej. returnSecureToken=true en el login
                // de Identity Toolkit); sin esto kotlinx.serialization los omite y Firebase no
                // devolvería refreshToken/expiresIn.
                encodeDefaults = true
                // Omite campos nulos al serializar: Firestore rechaza un `Value` con varias claves de
                // tipo a la vez, y sus documentos no deben llevar campos con null suelto (SPEC-0005).
                // Convive con encodeDefaults: los defaults NO nulos (returnSecureToken) sí se codifican.
                explicitNulls = false
            },
        )
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 20_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 20_000
    }
}

private const val FIREBASE_ID_TOKEN_PLUGIN = "FirebaseIdToken"

private object NapierKtorLogger : Logger {
    override fun log(message: String) {
        Napier.d(message, tag = "HttpClient")
    }
}
