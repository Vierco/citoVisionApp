package dev.lovelace.citovision.infrastructure.auth

import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.errors.AuthError
import dev.lovelace.citovision.infrastructure.auth.remote.IdentityToolkitAuthDataSource
import dev.lovelace.citovision.infrastructure.network.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class DesktopFirebaseAuthServiceTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    /** Reloj manipulable para provocar la caducidad del ID token sin esperar. */
    private class MutableClock(
        var current: Instant,
    ) : Clock {
        override fun now(): Instant = current
    }

    private fun service(
        status: HttpStatusCode,
        body: String,
    ): DesktopFirebaseAuthService {
        val engine = MockEngine { respond(content = body, status = status, headers = jsonHeaders) }
        val remote = IdentityToolkitAuthDataSource(createHttpClient(engine), apiKey = "test-key")
        return DesktopFirebaseAuthService(remote)
    }

    /**
     * Servicio cuyo endpoint de refresco responde algo distinto al de login: el `securetoken` es el que
     * renueva el ID token cuando el del login ha caducado.
     */
    private fun serviceWithRefresh(
        clock: Clock,
        signInBody: String,
        refreshStatus: HttpStatusCode,
        refreshBody: String,
    ): DesktopFirebaseAuthService {
        val engine =
            MockEngine { request ->
                if (request.url.host == "securetoken.googleapis.com") {
                    respond(content = refreshBody, status = refreshStatus, headers = jsonHeaders)
                } else {
                    respond(content = signInBody, status = HttpStatusCode.OK, headers = jsonHeaders)
                }
            }
        val remote = IdentityToolkitAuthDataSource(createHttpClient(engine), apiKey = "test-key")
        return DesktopFirebaseAuthService(remote, clock)
    }

    @Test
    fun `given valid credentials when signing in then emits the user and returns success`() =
        runTest {
            val service =
                service(
                    HttpStatusCode.OK,
                    """{"idToken":"id","refreshToken":"rt","expiresIn":"3600","localId":"uid-1","email":"a@b.com"}""",
                )

            val result = service.signInWithEmail("a@b.com", "secret")

            assertTrue(result is Result.Success)
            assertEquals("uid-1", result.value.uid)
            assertEquals("uid-1", service.currentUser.first()?.uid)
        }

    @Test
    fun `given wrong credentials when signing in then keeps no session`() =
        runTest {
            val service =
                service(
                    HttpStatusCode.BadRequest,
                    """{"error":{"code":400,"message":"INVALID_PASSWORD"}}""",
                )

            val result = service.signInWithEmail("a@b.com", "bad")

            assertEquals(Result.Failure(AuthError.InvalidCredentials), result)
            assertNull(service.currentUser.first())
        }

    @Test
    fun `given desktop when signing in with google then is not supported`() =
        runTest {
            val service = service(HttpStatusCode.OK, "{}")

            val result = service.signInWithGoogle("token")

            assertEquals(Result.Failure(AuthError.NotSupportedOnPlatform), result)
        }

    @Test
    fun `given guest access when signing in then emits a local guest session`() =
        runTest {
            val service = service(HttpStatusCode.OK, "{}")

            val result = service.signInAsGuest()

            assertTrue(result is Result.Success)
            assertTrue(result.value.isGuest)
            assertEquals(true, service.currentUser.first()?.isGuest)
        }

    @Test
    fun `given an active session when signing out then clears the current user`() =
        runTest {
            val service =
                service(
                    HttpStatusCode.OK,
                    """{"idToken":"id","refreshToken":"rt","expiresIn":"3600","localId":"uid-1"}""",
                )
            service.signInWithEmail("a@b.com", "secret")

            service.signOut()

            assertNull(service.currentUser.first())
        }

    @Test
    fun `given no session when asking for the id token then returns null`() =
        runTest {
            val service = service(HttpStatusCode.OK, "{}")

            assertNull(service.currentIdToken())
            service.signInAsGuest()
            assertNull(service.currentIdToken())
        }

    @Test
    fun `given a fresh session when asking for the id token then returns the one from the login`() =
        runTest {
            val service =
                service(
                    HttpStatusCode.OK,
                    """{"idToken":"id-1","refreshToken":"rt","expiresIn":"3600","localId":"uid-1"}""",
                )
            service.signInWithEmail("a@b.com", "secret")

            assertEquals("id-1", service.currentIdToken())
        }

    @Test
    fun `given an expired token when asking for the id token then renews it`() =
        runTest {
            val clock = MutableClock(Instant.fromEpochMilliseconds(0))
            val service =
                serviceWithRefresh(
                    clock = clock,
                    signInBody = """{"idToken":"id-1","refreshToken":"rt","expiresIn":"3600","localId":"uid-1"}""",
                    refreshStatus = HttpStatusCode.OK,
                    refreshBody =
                        """{"id_token":"id-2","refresh_token":"rt-2","expires_in":"3600","user_id":"uid-1"}""",
                )
            service.signInWithEmail("a@b.com", "secret")

            clock.current = clock.current + 2.hours

            assertEquals("id-2", service.currentIdToken())
            assertEquals("uid-1", service.currentUser.first()?.uid)
        }

    @Test
    fun `given a rejected refresh token when asking for the id token then ends the session`() =
        runTest {
            val clock = MutableClock(Instant.fromEpochMilliseconds(0))
            val service =
                serviceWithRefresh(
                    clock = clock,
                    signInBody = """{"idToken":"id-1","refreshToken":"rt","expiresIn":"3600","localId":"uid-1"}""",
                    refreshStatus = HttpStatusCode.BadRequest,
                    refreshBody = """{"error":{"code":400,"message":"TOKEN_EXPIRED"}}""",
                )
            service.signInWithEmail("a@b.com", "secret")

            clock.current = clock.current + 2.hours

            assertNull(service.currentIdToken())
            assertNull(service.currentUser.first())
        }
}
