package dev.lovelace.citovision.infrastructure.remote

import dev.lovelace.citovision.application.ports.AnalysisImageStore
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.infrastructure.network.createHttpClient
import dev.lovelace.citovision.infrastructure.persistence.database.AnalysisDao
import dev.lovelace.citovision.infrastructure.persistence.database.AnalysisEntity
import dev.lovelace.citovision.infrastructure.persistence.database.AnalysisWithCellCounts
import dev.lovelace.citovision.infrastructure.persistence.database.RemoteUploadOutboxDao
import dev.lovelace.citovision.infrastructure.persistence.database.RemoteUploadOutboxEntity
import dev.lovelace.citovision.infrastructure.remote.firestore.FirestoreAnalysisDataSource
import dev.lovelace.citovision.infrastructure.remote.storage.FirebaseStorageDataSource
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * El outbox se prueba con un fake determinista (assert sobre su estado) en vez de verificaciones Mokkery;
 * `AnalysisDao`/`AnalysisImageStore` se mockean (solo stubbing) y los DataSources remotos son reales sobre
 * un [MockEngine] que enruta por host (Storage vs Firestore), ya que son clases finales no mockeables.
 */
class RemoteAnalysisSyncImplTest {
    private val analysisDao = mock<AnalysisDao>()
    private val imageStore = mock<AnalysisImageStore>()
    private val outbox = FakeOutboxDao()
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun routingEngine(firestoreStatus: HttpStatusCode = HttpStatusCode.OK) =
        MockEngine { request ->
            when {
                request.url.host.startsWith("firebasestorage") ->
                    respond("""{"downloadTokens":"tok"}""", HttpStatusCode.OK, jsonHeaders)
                request.url.host.startsWith("firestore") ->
                    respond("{}", firestoreStatus, jsonHeaders)
                else -> respond("{}", HttpStatusCode.OK, jsonHeaders)
            }
        }

    private fun sync(engine: MockEngine): RemoteAnalysisSyncImpl {
        val client = createHttpClient(engine)
        return RemoteAnalysisSyncImpl(
            outboxDao = outbox,
            analysisDao = analysisDao,
            imageStore = imageStore,
            firestore = FirestoreAnalysisDataSource(client, projectId = "p", apiKey = "k"),
            storage = FirebaseStorageDataSource(client, bucket = "bkt"),
        )
    }

    private fun outboxEntry(id: String = "a1") =
        RemoteUploadOutboxEntity(
            analysisId = id,
            ownerUid = "u1",
            status = "PENDING",
            attempts = 0,
            lastError = null,
            createdAt = 100,
        )

    private fun analysisRow(
        id: String = "a1",
        imagePath: String?,
    ) = AnalysisWithCellCounts(
        analysis =
            AnalysisEntity(
                id = id,
                patient = "12-34",
                performedAt = 100,
                summary = "s",
                imagePath = imagePath,
            ),
        cellCounts = emptyList(),
    )

    @Test
    fun `given a pending entry with image when processing then uploads saves and removes it`() =
        runTest {
            outbox.upsert(outboxEntry())
            everySuspend { analysisDao.findById("a1") } returns analysisRow(imagePath = "/local/a1.png")
            everySuspend { imageStore.read("/local/a1.png") } returns Result.Success(byteArrayOf(1, 2, 3))

            val result = sync(routingEngine()).processPending()

            assertEquals(Result.Success(Unit), result)
            assertEquals(0, outbox.count())
        }

    @Test
    fun `given a pending entry without image when processing then skips upload and removes it`() =
        runTest {
            outbox.upsert(outboxEntry())
            everySuspend { analysisDao.findById("a1") } returns analysisRow(imagePath = null)

            val result = sync(routingEngine()).processPending()

            assertEquals(Result.Success(Unit), result)
            assertEquals(0, outbox.count())
        }

    @Test
    fun `given firestore keeps failing when processing then marks the entry failed and returns failure`() =
        runTest {
            outbox.upsert(outboxEntry())
            everySuspend { analysisDao.findById("a1") } returns analysisRow(imagePath = null)

            val result = sync(routingEngine(HttpStatusCode.InternalServerError)).processPending()

            assertTrue(result is Result.Failure)
            assertEquals("FAILED", outbox.findById("a1")?.status)
        }

    @Test
    fun `given the local analysis is gone when processing then drops the entry`() =
        runTest {
            outbox.upsert(outboxEntry())
            everySuspend { analysisDao.findById("a1") } returns null

            val result = sync(routingEngine()).processPending()

            assertEquals(Result.Success(Unit), result)
            assertEquals(0, outbox.count())
        }

    @Test
    fun `given an analysis id when enqueuing then stores a pending entry`() =
        runTest {
            sync(routingEngine()).enqueue("a1", "u1")

            val stored = outbox.findById("a1")
            assertEquals("PENDING", stored?.status)
            assertEquals(0, stored?.attempts)
        }
}

private class FakeOutboxDao : RemoteUploadOutboxDao {
    private val entries = linkedMapOf<String, RemoteUploadOutboxEntity>()

    override suspend fun upsert(entry: RemoteUploadOutboxEntity) {
        entries[entry.analysisId] = entry
    }

    override suspend fun getAll(): List<RemoteUploadOutboxEntity> = entries.values.sortedBy { it.createdAt }

    override suspend fun findById(analysisId: String): RemoteUploadOutboxEntity? = entries[analysisId]

    override suspend fun deleteById(analysisId: String) {
        entries.remove(analysisId)
    }

    override suspend fun count(): Int = entries.size
}
