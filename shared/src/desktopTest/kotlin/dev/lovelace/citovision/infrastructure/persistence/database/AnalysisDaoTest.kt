package dev.lovelace.citovision.infrastructure.persistence.database

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test de integración contra una base de datos Room **real** en un fichero temporal (no se mockea Room).
 * Verifica el orden por fecha descendente y, sobre todo, el borrado en cascada del conteo celular.
 */
class AnalysisDaoTest {
    private lateinit var databaseFile: File
    private lateinit var database: AppDatabase
    private lateinit var dao: AnalysisDao

    private fun analysisEntity(
        id: String,
        performedAt: Long,
    ) = AnalysisEntity(
        id = id,
        patient = "PAC-2026-001",
        performedAt = performedAt,
        summary = "Resumen",
        imagePath = "/tmp/$id.png",
    )

    private fun cellCounts(
        analysisId: String,
        count: Int,
    ) = (0 until count).map { index ->
        CellCountEntity(analysisId = analysisId, position = index, name = "Célula $index", value = "$index%")
    }

    @BeforeTest
    fun setUp() {
        databaseFile = File.createTempFile("citovision-test", ".db").also { it.delete() }
        database = createAppDatabase(Room.databaseBuilder<AppDatabase>(name = databaseFile.absolutePath))
        dao = database.analysisDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        databaseFile.delete()
    }

    @Test
    fun `given several analyses when observing then they come ordered by date descending`() =
        runTest {
            // Given
            dao.insertAnalysisWithCellCounts(analysisEntity("old", 1_000), cellCounts("old", 2))
            dao.insertAnalysisWithCellCounts(analysisEntity("new", 2_000), cellCounts("new", 3))

            // When
            val rows = dao.observeAll().first()

            // Then
            assertEquals(listOf("new", "old"), rows.map { it.analysis.id })
            assertEquals(3, rows.first().cellCounts.size)
        }

    @Test
    fun `given an analysis with cell counts when deleting it then the counts are removed in cascade`() =
        runTest {
            // Given
            dao.insertAnalysisWithCellCounts(analysisEntity("1", 1_000), cellCounts("1", 4))
            assertEquals(
                4,
                dao
                    .observeAll()
                    .first()
                    .first()
                    .cellCounts.size,
            )

            // When
            val deletedRows = dao.deleteById("1")

            // Then
            assertEquals(1, deletedRows)
            assertTrue(dao.observeAll().first().isEmpty())
        }

    @Test
    fun `given a missing analysis when deleting then no rows are affected`() =
        runTest {
            // When
            val deletedRows = dao.deleteById("does-not-exist")

            // Then
            assertEquals(0, deletedRows)
        }

    @Test
    fun `given a stored analysis when asking for its image path then it is returned`() =
        runTest {
            // Given
            dao.insertAnalysisWithCellCounts(analysisEntity("1", 1_000), emptyList())

            // When / Then
            assertEquals("/tmp/1.png", dao.findImagePath("1"))
            assertNull(dao.findImagePath("does-not-exist"))
        }
}
