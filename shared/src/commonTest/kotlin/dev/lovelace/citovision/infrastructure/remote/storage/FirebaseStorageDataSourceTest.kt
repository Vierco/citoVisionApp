package dev.lovelace.citovision.infrastructure.remote.storage

import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.errors.RemoteAnalysisError
import dev.lovelace.citovision.infrastructure.network.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FirebaseStorageDataSourceTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun dataSource(
        status: HttpStatusCode,
        body: String,
        onRequest: (HttpRequestData) -> Unit = {},
    ): FirebaseStorageDataSource {
        val engine =
            MockEngine { request ->
                onRequest(request)
                respond(content = body, status = status, headers = jsonHeaders)
            }
        return FirebaseStorageDataSource(createHttpClient(engine), bucket = "bkt")
    }

    @Test
    fun `given an image when uploading then returns a download url with the token`() =
        runTest {
            var name: String? = null
            var mediaType: String? = null
            val ds =
                dataSource(
                    HttpStatusCode.OK,
                    """{"name":"analyses/u1/a1.jpg","bucket":"bkt","downloadTokens":"tok-123"}""",
                ) { request ->
                    name = request.url.parameters["name"]
                    mediaType = request.body.contentType?.contentType
                }

            val result = ds.uploadImage("u1", "a1", byteArrayOf(1, 2, 3), "image/jpeg")

            assertTrue(result is Result.Success)
            assertEquals("analyses/u1/a1.jpg", name)
            assertEquals("image", mediaType)
            assertTrue(result.value.contains("analyses%2Fu1%2Fa1.jpg"))
            assertTrue(result.value.contains("alt=media"))
            assertTrue(result.value.contains("token=tok-123"))
        }

    @Test
    fun `given a png when uploading then uses a png object path`() =
        runTest {
            var name: String? = null
            val ds =
                dataSource(HttpStatusCode.OK, """{"downloadTokens":"t"}""") { request ->
                    name = request.url.parameters["name"]
                }

            ds.uploadImage("u1", "a1", byteArrayOf(1), "image/png")

            assertTrue(name?.endsWith(".png") == true)
        }

    @Test
    fun `given a response without token when uploading then fails`() =
        runTest {
            val ds = dataSource(HttpStatusCode.OK, """{"name":"analyses/u1/a1.jpg","bucket":"bkt"}""")

            val result = ds.uploadImage("u1", "a1", byteArrayOf(1), "image/png")

            assertTrue(result is Result.Failure && result.error is RemoteAnalysisError.Unknown)
        }

    @Test
    fun `given a forbidden response when uploading then maps to unauthorized`() =
        runTest {
            val ds = dataSource(HttpStatusCode.Forbidden, "{}")

            val result = ds.uploadImage("u1", "a1", byteArrayOf(1), "image/jpeg")

            assertEquals(Result.Failure(RemoteAnalysisError.Unauthorized), result)
        }

    @Test
    fun `given a network failure when uploading then maps to network error`() =
        runTest {
            val ds = failingDataSource(IOException("no network"))

            val result = ds.uploadImage("u1", "a1", byteArrayOf(1), "image/jpeg")

            assertEquals(Result.Failure(RemoteAnalysisError.Network), result)
        }

    /** Solo un fallo de E/S es "sin conexión"; lo demás se admite como desconocido. */
    @Test
    fun `given an unexpected failure when uploading then maps to unknown instead of network`() =
        runTest {
            val ds = failingDataSource(IllegalStateException("bug"))

            val result = ds.uploadImage("u1", "a1", byteArrayOf(1), "image/jpeg")

            assertTrue(result is Result.Failure)
            assertIs<RemoteAnalysisError.Unknown>(result.error)
        }

    private fun failingDataSource(failure: Throwable): FirebaseStorageDataSource {
        val engine = MockEngine { throw failure }
        return FirebaseStorageDataSource(createHttpClient(engine), bucket = "bkt")
    }
}
