package dev.lovelace.citovision.infrastructure.network

import dev.lovelace.citovision.application.ports.AuthTokenProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * El ID token es lo único que separa a los dos clientes: sin él, las reglas de Firestore/Storage
 * rechazan la petición; con él en el cliente equivocado, la contraseña del login viajaría junto a una
 * credencial que aún no debería existir.
 */
class HttpClientFactoryTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private class FixedTokenProvider(
        private val token: String?,
    ) : AuthTokenProvider {
        override suspend fun currentIdToken(): String? = token
    }

    private fun engineCapturing(captured: MutableList<String?>): MockEngine =
        MockEngine { request ->
            captured += request.headers[HttpHeaders.Authorization]
            respond(content = "{}", status = HttpStatusCode.OK, headers = jsonHeaders)
        }

    @Test
    fun `given an account session when calling then attaches the bearer token`() =
        runTest {
            val captured = mutableListOf<String?>()
            val client = createAuthorizedHttpClient(engineCapturing(captured), FixedTokenProvider("id-token"))

            client.get("https://firestore.googleapis.com/v1/documents")

            assertEquals("Bearer id-token", captured.single())
        }

    @Test
    fun `given no session when calling then sends no authorization header`() =
        runTest {
            val captured = mutableListOf<String?>()
            val client = createAuthorizedHttpClient(engineCapturing(captured), FixedTokenProvider(null))

            client.get("https://firestore.googleapis.com/v1/documents")

            assertNull(captured.single())
        }

    @Test
    fun `given the plain client when calling then never sends authorization`() =
        runTest {
            val captured = mutableListOf<String?>()
            val client = createHttpClient(engineCapturing(captured))

            client.get("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword")

            assertNull(captured.single())
        }
}
