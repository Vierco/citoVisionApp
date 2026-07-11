package dev.lovelace.citovision.infrastructure.persistence.database

import androidx.room.Room
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Test de integración de la cola de sincronización contra una base de datos Room **real** en fichero
 * temporal. Verifica el orden por creación, la idempotencia del `@Upsert` sobre `analysisId` y el borrado.
 */
class RemoteUploadOutboxDaoTest {
    private lateinit var databaseFile: File
    private lateinit var database: AppDatabase
    private lateinit var dao: RemoteUploadOutboxDao

    private fun entry(
        id: String,
        createdAt: Long = 0,
        status: String = "PENDING",
        attempts: Int = 0,
    ) = RemoteUploadOutboxEntity(
        analysisId = id,
        ownerUid = "u1",
        status = status,
        attempts = attempts,
        lastError = null,
        createdAt = createdAt,
    )

    @BeforeTest
    fun setUp() {
        databaseFile = File.createTempFile("citovision-outbox-test", ".db").also { it.delete() }
        database = createAppDatabase(Room.databaseBuilder<AppDatabase>(name = databaseFile.absolutePath))
        dao = database.outboxDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        databaseFile.delete()
    }

    @Test
    fun `given entries when getting all then they come ordered by creation ascending`() =
        runTest {
            dao.upsert(entry("new", createdAt = 2_000))
            dao.upsert(entry("old", createdAt = 1_000))

            assertEquals(listOf("old", "new"), dao.getAll().map { it.analysisId })
        }

    @Test
    fun `given the same analysis when upserting twice then it replaces keeping a single row`() =
        runTest {
            dao.upsert(entry("a1", status = "PENDING", attempts = 0))
            dao.upsert(entry("a1", status = "FAILED", attempts = 2))

            assertEquals(1, dao.count())
            val stored = dao.findById("a1")
            assertEquals("FAILED", stored?.status)
            assertEquals(2, stored?.attempts)
        }

    @Test
    fun `given an entry when deleting it then it is removed`() =
        runTest {
            dao.upsert(entry("a1"))

            dao.deleteById("a1")

            assertEquals(0, dao.count())
            assertNull(dao.findById("a1"))
        }
}
