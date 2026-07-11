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

class DesktopFirebaseAuthServiceTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun service(
        status: HttpStatusCode,
        body: String,
    ): DesktopFirebaseAuthService {
        val engine = MockEngine { respond(content = body, status = status, headers = jsonHeaders) }
        val remote = IdentityToolkitAuthDataSource(createHttpClient(engine), apiKey = "test-key")
        return DesktopFirebaseAuthService(remote)
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
}
