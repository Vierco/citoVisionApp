package dev.lovelace.citovision.infrastructure.persistence.mappers

import dev.lovelace.citovision.domain.entities.Analysis
import dev.lovelace.citovision.domain.entities.CellCount
import dev.lovelace.citovision.domain.entities.DetectionLevel
import dev.lovelace.citovision.domain.entities.Priority
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
                            name = "Neutrófilo segmentado",
                            count = 1,
                            confidences = "0.85",
                        ),
                        CellCountEntity(
                            id = 1,
                            analysisId = "1",
                            position = 0,
                            name = "Linfocito",
                            count = 3,
                            confidences = "0.9,0.8,0.7",
                        ),
                    ),
            )

        // When
        val analysis = row.toDomain()

        // Then
        assertEquals(
            listOf(
                CellCount("Linfocito", count = 3, confidences = listOf(0.9f, 0.8f, 0.7f)),
                CellCount("Neutrófilo segmentado", count = 1, confidences = listOf(0.85f)),
            ),
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
                cellCounts =
                    listOf(
                        CellCount("Linfocito", count = 3, confidences = listOf(0.9f, 0.8f, 0.7f)),
                        CellCount("Artefacto", count = 2, confidences = emptyList()),
                    ),
            )

        // When
        val entities = analysis.toCellCountEntities()

        // Then
        assertEquals(entity, analysis.toEntity())
        assertEquals(listOf(0, 1), entities.map { it.position })
        assertEquals(listOf("Linfocito", "Artefacto"), entities.map { it.name })
        assertEquals(listOf("1", "1"), entities.map { it.analysisId })
        assertEquals(listOf(3, 2), entities.map { it.count })
        // Las confianzas viajan como CSV; las clases no celulares quedan vacías.
        assertEquals(listOf("0.9,0.8,0.7", ""), entities.map { it.confidences })
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `given a priority when mapping in both directions then it round-trips`() {
        // Entidad → dominio
        val row = AnalysisWithCellCounts(analysis = entity.copy(priority = "ALTA"), cellCounts = emptyList())
        assertEquals(Priority.ALTA, row.toDomain().priority)

        // Dominio → entidad
        val analysis =
            Analysis(
                id = "1",
                patient = "PAC-2026-001",
                performedAt = Instant.fromEpochMilliseconds(1_700_000_000_000),
                summary = "Resumen",
                imagePath = "/tmp/a.png",
                cellCounts = emptyList(),
                priority = Priority.ALTA,
            )
        assertEquals("ALTA", analysis.toEntity().priority)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `given a detection level when mapping in both directions then it round-trips`() {
        val analysis =
            Analysis(
                id = "1",
                patient = "PAC-2026-001",
                performedAt = Instant.fromEpochMilliseconds(1_700_000_000_000),
                summary = "Resumen",
                imagePath = "/tmp/a.png",
                cellCounts =
                    listOf(
                        CellCount("Mielocito", count = 1, confidences = listOf(0.97f)),
                        CellCount(
                            "Promielocito",
                            count = 1,
                            confidences = listOf(0.09f),
                            level = DetectionLevel.LOW_CONFIDENCE_REVIEW,
                        ),
                    ),
            )

        val entities = analysis.toCellCountEntities()
        assertEquals(listOf("STANDARD", "LOW_CONFIDENCE_REVIEW"), entities.map { it.level })

        val row = AnalysisWithCellCounts(analysis = entity, cellCounts = entities)
        assertEquals(analysis.cellCounts, row.toDomain().cellCounts)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `given a row migrated from v4 when mapping to domain then the level falls back to standard`() {
        // Las filas anteriores a la política de umbrales se generaron con el umbral único de 0.25.
        val row =
            AnalysisWithCellCounts(
                analysis = entity,
                cellCounts =
                    listOf(
                        CellCountEntity(
                            id = 1,
                            analysisId = "1",
                            position = 0,
                            name = "Linfocito",
                            count = 1,
                            confidences = "0.9",
                            level = "",
                        ),
                    ),
            )

        assertEquals(
            DetectionLevel.STANDARD,
            row
                .toDomain()
                .cellCounts
                .single()
                .level,
        )
    }
}
