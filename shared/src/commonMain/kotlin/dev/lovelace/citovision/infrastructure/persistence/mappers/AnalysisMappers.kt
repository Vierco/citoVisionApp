package dev.lovelace.citovision.infrastructure.persistence.mappers

import dev.lovelace.citovision.domain.entities.Analysis
import dev.lovelace.citovision.domain.entities.CellCount
import dev.lovelace.citovision.infrastructure.persistence.database.AnalysisEntity
import dev.lovelace.citovision.infrastructure.persistence.database.AnalysisWithCellCounts
import dev.lovelace.citovision.infrastructure.persistence.database.CellCountEntity
import kotlin.time.Instant

/**
 * Mapeo explícito entre las entidades de Room y el dominio. Room no garantiza el orden de la relación,
 * así que el conteo celular se reordena por `position` (SPEC-0004 RN-6).
 */
fun AnalysisWithCellCounts.toDomain(): Analysis =
    Analysis(
        id = analysis.id,
        patient = analysis.patient,
        performedAt = Instant.fromEpochMilliseconds(analysis.performedAt),
        summary = analysis.summary,
        imagePath = analysis.imagePath,
        cellCounts =
            cellCounts
                .sortedBy { it.position }
                .map { CellCount(name = it.name, value = it.value) },
    )

fun Analysis.toEntity(): AnalysisEntity =
    AnalysisEntity(
        id = id,
        patient = patient,
        performedAt = performedAt.toEpochMilliseconds(),
        summary = summary,
        imagePath = imagePath,
    )

fun Analysis.toCellCountEntities(): List<CellCountEntity> =
    cellCounts.mapIndexed { index, cellCount ->
        CellCountEntity(
            analysisId = id,
            position = index,
            name = cellCount.name,
            value = cellCount.value,
        )
    }
