package dev.lovelace.citovision.infrastructure.persistence.mappers

import dev.lovelace.citovision.domain.entities.Analysis
import dev.lovelace.citovision.domain.entities.CellCount
import dev.lovelace.citovision.infrastructure.persistence.database.AnalysisEntity
import dev.lovelace.citovision.infrastructure.persistence.database.AnalysisWithCellCounts
import dev.lovelace.citovision.infrastructure.persistence.database.CellCountEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class AnalysisMappersTest {
    private val entity =
        AnalysisEntity(
            id = "1",
            patient = "PAC-2026-001",
            performedAt = 1_700_000_000_000,
            summary = "Resumen",
            imagePath = "/tmp/a.png",
        )

    @OptIn(ExperimentalTime::class)
    @Test
    fun `given cell counts out of order when mapping to domain then they are sorted by position`() {
        // Given: Room no garantiza el orden de la relación
        val row =
            AnalysisWithCellCounts(
                analysis = entity,
                cellCounts =
                    listOf(
                        CellCountEntity(
                            id = 2,
                            analysisId = "1",
                            position = 1,
                            name = "Neutrófilos",
                            value = "60%",
                        ),
                        CellCountEntity(
                            id = 1,
                            analysisId = "1",
                            position = 0,
                            name = "Leucocitos",
                            value = "7.500/µL",
                        ),
                    ),
            )

        // When
        val analysis = row.toDomain()

        // Then
        assertEquals(
            listOf(CellCount("Leucocitos", "7.500/µL"), CellCount("Neutrófilos", "60%")),
            analysis.cellCounts,
        )
        assertEquals(Instant.fromEpochMilliseconds(1_700_000_000_000), analysis.performedAt)
    }

    @Test
    fun `given an analysis without image when mapping to domain then imagePath is null`() {
        // Given
        val row = AnalysisWithCellCounts(analysis = entity.copy(imagePath = null), cellCounts = emptyList())

        // When
        val analysis = row.toDomain()

        // Then
        assertNull(analysis.imagePath)
        assertEquals(emptyList(), analysis.cellCounts)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `given a domain analysis when mapping to entities then position preserves the list order`() {
        // Given
        val analysis =
            Analysis(
                id = "1",
                patient = "PAC-2026-001",
                performedAt = Instant.fromEpochMilliseconds(1_700_000_000_000),
                summary = "Resumen",
                imagePath = "/tmp/a.png",
                cellCounts = listOf(CellCount("Leucocitos", "7.500/µL"), CellCount("Neutrófilos", "60%")),
            )

        // When
        val entities = analysis.toCellCountEntities()

        // Then
        assertEquals(entity, analysis.toEntity())
        assertEquals(listOf(0, 1), entities.map { it.position })
        assertEquals(listOf("Leucocitos", "Neutrófilos"), entities.map { it.name })
        assertEquals(listOf("1", "1"), entities.map { it.analysisId })
    }
}
