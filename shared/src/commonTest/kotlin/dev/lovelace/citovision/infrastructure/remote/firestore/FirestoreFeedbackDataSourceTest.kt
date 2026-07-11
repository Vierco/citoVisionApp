package dev.lovelace.citovision.infrastructure.remote.firestore

import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.Feedback
import dev.lovelace.citovision.domain.errors.RemoteFeedbackError
import dev.lovelace.citovision.infrastructure.network.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class FirestoreFeedbackDataSourceTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun feedback(ownerUid: String?) =
        Feedback(
            email = "doc@clinica.com",
            message = "Buen trabajo",
            createdAt = Instant.fromEpochMilliseconds(2000),
            ownerUid = ownerUid,
        )

    private fun dataSource(
        status: HttpStatusCode,
        body: String,
        onRequestBody: (String?) -> Unit = {},
    ): FirestoreFeedbackDataSource {
        val engine =
            MockEngine { request ->
                onRequestBody((request.body as? TextContent)?.text)
                respond(content = body, status = status, headers = jsonHeaders)
            }
        return FirestoreFeedbackDataSource(createHttpClient(engine), projectId = "p", apiKey = "k")
    }

    @Test
    fun `given feedback with account when submitting then sends typed document with ownerUid`() =
        runTest {
            var capturedBody: String? = null
            val ds = dataSource(HttpStatusCode.OK, "{}") { capturedBody = it }

            val result = ds.submit(feedback(ownerUid = "u1"))

            assertEquals(Result.Success(Unit), result)
            val body = capturedBody.orEmpty()
            assertTrue(body.contains("\"stringValue\":\"doc@clinica.com\""))
            assertTrue(body.contains("\"stringValue\":\"Buen trabajo\""))
            assertTrue(body.contains("\"integerValue\":\"2000\""))
            assertTrue(body.contains("\"stringValue\":\"u1\""))
            assertFalse(body.contains("nullValue"))
        }

    @Test
    fun `given feedback from a guest when submitting then omits ownerUid`() =
        runTest {
            var capturedBody: String? = null
            val ds = dataSource(HttpStatusCode.OK, "{}") { capturedBody = it }

            ds.submit(feedback(ownerUid = null))

            assertFalse(capturedBody.orEmpty().contains("ownerUid"))
        }

    @Test
    fun `given a server error when submitting then maps to a failure`() =
        runTest {
            val ds = dataSource(HttpStatusCode.InternalServerError, """{"error":{"code":500}}""")

            val result = ds.submit(feedback(ownerUid = "u1"))

            assertTrue(result is Result.Failure)
            assertTrue(result.error is RemoteFeedbackError.Unknown)
        }
}
