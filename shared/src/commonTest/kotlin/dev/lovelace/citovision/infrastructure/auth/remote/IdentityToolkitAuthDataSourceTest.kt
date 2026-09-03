package dev.lovelace.citovision.infrastructure.auth.remote

import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.errors.AuthError
import dev.lovelace.citovision.infrastructure.network.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class IdentityToolkitAuthDataSourceTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun dataSource(
        status: HttpStatusCode,
        body: String,
    ): IdentityToolkitAuthDataSource {
        val engine = MockEngine { respond(content = body, status = status, headers = jsonHeaders) }
        return IdentityToolkitAuthDataSource(createHttpClient(engine), apiKey = "test-key")
    }

    @Test
    fun `given valid credentials when signing in then returns the session`() =
        runTest {
            val ds =
                dataSource(
                    HttpStatusCode.OK,
                    """{"idToken":"id","refreshToken":"rt","expiresIn":"3600","localId":"uid-1","email":"a@b.com"}""",
                )

            val result = ds.signInWithPassword("a@b.com", "secret")

            assertTrue(result is Result.Success)
            assertEquals("id", result.value.idToken)
            assertEquals("uid-1", result.value.localId)
        }

    @Test
    fun `given wrong password when signing in then maps to invalid credentials`() =
        runTest {
            val ds =
                dataSource(
                    HttpStatusCode.BadRequest,
                    """{"error":{"code":400,"message":"INVALID_PASSWORD"}}""",
                )

            val result = ds.signInWithPassword("a@b.com", "bad")

            assertEquals(Result.Failure(AuthError.InvalidCredentials), result)
        }

    @Test
    fun `given too many attempts when signing in then maps to too many requests`() =
        runTest {
            val ds =
                dataSource(
                    HttpStatusCode.BadRequest,
                    """{"error":{"code":400,"message":"TOO_MANY_ATTEMPTS_TRY_LATER : try again"}}""",
                )

            val result = ds.signInWithPassword("a@b.com", "x")

            assertEquals(Result.Failure(AuthError.TooManyRequests), result)
        }

    @Test
    fun `given an existing email when requesting reset then succeeds`() =
        runTest {
            val ds = dataSource(HttpStatusCode.OK, """{"email":"a@b.com"}""")

            val result = ds.sendPasswordResetEmail("a@b.com")

            assertEquals(Result.Success(Unit), result)
        }

    @Test
    fun `given an unknown email when requesting reset then maps to user not found`() =
        runTest {
            val ds =
                dataSource(
                    HttpStatusCode.BadRequest,
                    """{"error":{"code":400,"message":"EMAIL_NOT_FOUND"}}""",
                )

            val result = ds.sendPasswordResetEmail("x@y.com")

            assertEquals(Result.Failure(AuthError.UserNotFound), result)
        }

    @Test
    fun `given a valid refresh token when refreshing then returns a new id token`() =
        runTest {
            val ds =
                dataSource(
                    HttpStatusCode.OK,
                    """{"id_token":"newId","refresh_token":"newRt","expires_in":"3600","user_id":"uid-1"}""",
                )

            val result = ds.refreshIdToken("rt")

            assertTrue(result is Result.Success)
            assertEquals("newId", result.value.idToken)
            assertEquals("uid-1", result.value.userId)
        }

    @Test
    fun `given sign in when building the request then sends returnSecureToken`() =
        runTest {
            var capturedBody: String? = null
            val engine =
                MockEngine { request ->
                    capturedBody = (request.body as? TextContent)?.text
                    respond(
                        content = """{"idToken":"id","refreshToken":"rt","expiresIn":"3600","localId":"uid-1"}""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders,
                    )
                }
            val ds = IdentityToolkitAuthDataSource(createHttpClient(engine), apiKey = "test-key")

            ds.signInWithPassword("a@b.com", "secret")

            assertTrue(capturedBody?.contains("\"returnSecureToken\":true") == true)
        }

    @Test
    fun `given a network failure when signing in then maps to network error`() =
        runTest {
            val engine = MockEngine { throw IOException("no network") }
            val ds = IdentityToolkitAuthDataSource(createHttpClient(engine), apiKey = "test-key")

            val result = ds.signInWithPassword("a@b.com", "secret")

            assertEquals(Result.Failure(AuthError.Network), result)
        }

    /**
     * Antes cualquier excepción inesperada se anunciaba como falta de conexión, y eso mandaba al usuario
     * —y a quien depuraba— a mirar la red cuando el fallo estaba en otro sitio (así se camufló KTOR-7943).
     */
    @Test
    fun `given an unexpected failure when signing in then maps to unknown instead of network`() =
        runTest {
            val engine = MockEngine { throw IllegalStateException("bug") }
            val ds = IdentityToolkitAuthDataSource(createHttpClient(engine), apiKey = "test-key")

            val result = ds.signInWithPassword("a@b.com", "secret")

            // Se comprueba el caso, no el nombre concreto de la excepción: ese detalle depende de si el
            // motor de Ktor la envuelve, y no es lo que esta regla protege.
            assertTrue(result is Result.Failure)
            assertIs<AuthError.Unknown>(result.error)
        }
}
