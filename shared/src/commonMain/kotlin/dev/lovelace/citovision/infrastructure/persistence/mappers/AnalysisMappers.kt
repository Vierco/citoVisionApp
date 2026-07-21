package dev.lovelace.citovision.infrastructure.persistence.mappers

import dev.lovelace.citovision.domain.entities.Analysis
import dev.lovelace.citovision.domain.entities.CellCount
import dev.lovelace.citovision.domain.entities.DetectionLevel
import dev.lovelace.citovision.domain.entities.Priority
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
                .map {
                    CellCount(
                        name = it.name,
                        count = it.count,
                        confidences = it.confidences.toConfidenceList(),
                        level = it.level.toDetectionLevel(),
                    )
                },
        priority = Priority.fromName(analysis.priority),
        sampleName = analysis.sampleName,
    )

fun Analysis.toEntity(): AnalysisEntity =
    AnalysisEntity(
        id = id,
        patient = patient,
        performedAt = performedAt.toEpochMilliseconds(),
        summary = summary,
        imagePath = imagePath,
        priority = priority.name,
        sampleName = sampleName,
    )

fun Analysis.toCellCountEntities(): List<CellCountEntity> =
    cellCounts.mapIndexed { index, cellCount ->
        CellCountEntity(
            analysisId = id,
            position = index,
            name = cellCount.name,
            count = cellCount.count,
            confidences = cellCount.confidences.toCsv(),
            level = cellCount.level.name,
        )
    }

/** Serialización de las confianzas por célula como CSV con punto decimal (independiente de la localización). */
private const val CONFIDENCE_SEPARATOR = ","

private fun List<Float>.toCsv(): String = joinToString(CONFIDENCE_SEPARATOR)

private fun String.toConfidenceList(): List<Float> =
    if (isEmpty()) emptyList() else split(CONFIDENCE_SEPARATOR).map { it.toFloat() }

/** Un valor desconocido cae a `STANDARD`: mejor mostrar la entrada que perderla por un dato ilegible. */
private fun String.toDetectionLevel(): DetectionLevel =
    DetectionLevel.entries.firstOrNull { it.name == this } ?: DetectionLevel.STANDARD
