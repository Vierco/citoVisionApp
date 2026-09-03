package dev.lovelace.citovision.infrastructure.remote.firestore

import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.Analysis
import dev.lovelace.citovision.domain.entities.CellCount
import dev.lovelace.citovision.domain.errors.RemoteAnalysisError
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class FirestoreAnalysisDataSourceTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private val analysis =
        Analysis(
            id = "a1",
            patient = "12-34",
            performedAt = Instant.fromEpochMilliseconds(2000),
            summary = "resumen",
            imagePath = "/local/a1.png",
            cellCounts = listOf(CellCount("Leucocitos", count = 1, confidences = listOf(0.9f))),
        )

    private fun dataSource(
        status: HttpStatusCode,
        body: String,
        onRequestBody: (String?) -> Unit = {},
    ): FirestoreAnalysisDataSource {
        val engine =
            MockEngine { request ->
                onRequestBody((request.body as? TextContent)?.text)
                respond(content = body, status = status, headers = jsonHeaders)
            }
        return FirestoreAnalysisDataSource(createHttpClient(engine), projectId = "p", apiKey = "k")
    }

    @Test
    fun `given an analysis when saving then sends a firestore typed document`() =
        runTest {
            var capturedBody: String? = null
            val ds = dataSource(HttpStatusCode.OK, "{}") { capturedBody = it }

            val result = ds.saveAnalysis(ownerUid = "u1", analysis = analysis, imageUrl = "https://img/a1")

            assertEquals(Result.Success(Unit), result)
            val body = capturedBody.orEmpty()
            assertTrue(body.contains("\"stringValue\":\"u1\""))
            assertTrue(body.contains("\"integerValue\":\"2000\""))
            assertTrue(body.contains("\"arrayValue\""))
            assertTrue(body.contains("\"imageUrl\""))
            assertFalse(body.contains("nullValue"))
        }

    @Test
    fun `given an analysis without image when saving then omits imageUrl`() =
        runTest {
            var capturedBody: String? = null
            val ds = dataSource(HttpStatusCode.OK, "{}") { capturedBody = it }

            ds.saveAnalysis(ownerUid = "u1", analysis = analysis, imageUrl = null)

            assertFalse(capturedBody.orEmpty().contains("imageUrl"))
        }

    @Test
    fun `given documents when querying then returns them sorted by date descending`() =
        runTest {
            val body =
                """
                [
                  {"document":{"name":"projects/p/databases/(default)/documents/analyses/old","fields":{"performedAt":{"integerValue":"1000"}}}},
                  {"document":{"name":"projects/p/databases/(default)/documents/analyses/new","fields":{"performedAt":{"integerValue":"2000"}}}}
                ]
                """.trimIndent()
            val ds = dataSource(HttpStatusCode.OK, body)

            val result = ds.queryByPatient("u1", "12-34")

            assertTrue(result is Result.Success)
            assertEquals(listOf("new", "old"), result.value.map { it.id })
        }

    @Test
    fun `given no matches when querying then returns an empty list`() =
        runTest {
            val ds = dataSource(HttpStatusCode.OK, """[{"readTime":"2026-01-01T00:00:00Z"}]""")

            val result = ds.queryByPatient("u1", "99-99")

            assertEquals(Result.Success(emptyList()), result)
        }

    @Test
    fun `given repeated codes when listing patients then returns them once and sorted`() =
        runTest {
            var capturedBody: String? = null
            val body =
                """
                [
                  {"document":{"name":"projects/p/databases/(default)/documents/analyses/a1","fields":{"patientCode":{"stringValue":"20-26"}}}},
                  {"document":{"name":"projects/p/databases/(default)/documents/analyses/a2","fields":{"patientCode":{"stringValue":"12-34"}}}},
                  {"document":{"name":"projects/p/databases/(default)/documents/analyses/a3","fields":{"patientCode":{"stringValue":"20-26"}}}}
                ]
                """.trimIndent()
            val ds = dataSource(HttpStatusCode.OK, body) { capturedBody = it }

            val result = ds.queryPatientCodes("u1")

            assertTrue(result is Result.Success)
            assertEquals(listOf("12-34", "20-26"), result.value)
            // Proyección: la consulta pide solo el código, y filtra únicamente por dueño (RN-3).
            assertTrue(capturedBody.orEmpty().contains("\"select\""))
            assertFalse(capturedBody.orEmpty().contains("compositeFilter"))
        }

    @Test
    fun `given a forbidden response when querying then maps to unauthorized`() =
        runTest {
            val ds = dataSource(HttpStatusCode.Forbidden, """{"error":{"code":403,"message":"denied"}}""")

            val result = ds.queryByPatient("u1", "12-34")

            assertEquals(Result.Failure(RemoteAnalysisError.Unauthorized), result)
        }

    @Test
    fun `given a network failure when saving then maps to network error`() =
        runTest {
            val ds = failingDataSource(IOException("no network"))

            val result = ds.saveAnalysis(ownerUid = "u1", analysis = analysis, imageUrl = "https://img/a1")

            assertEquals(Result.Failure(RemoteAnalysisError.Network), result)
        }

    /** Solo un fallo de E/S es "sin conexión"; lo demás se admite como desconocido. */
    @Test
    fun `given an unexpected failure when saving then maps to unknown instead of network`() =
        runTest {
            val ds = failingDataSource(IllegalStateException("bug"))

            val result = ds.saveAnalysis(ownerUid = "u1", analysis = analysis, imageUrl = "https://img/a1")

            assertTrue(result is Result.Failure)
            assertIs<RemoteAnalysisError.Unknown>(result.error)
        }

    private fun failingDataSource(failure: Throwable): FirestoreAnalysisDataSource {
        val engine = MockEngine { throw failure }
        return FirestoreAnalysisDataSource(createHttpClient(engine), projectId = "p", apiKey = "k")
    }
}
