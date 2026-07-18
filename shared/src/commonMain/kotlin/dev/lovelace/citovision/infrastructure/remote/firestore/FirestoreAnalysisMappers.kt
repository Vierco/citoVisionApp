package dev.lovelace.citovision.infrastructure.remote.firestore

import dev.lovelace.citovision.domain.entities.Analysis
import dev.lovelace.citovision.domain.entities.CellCount
import dev.lovelace.citovision.domain.entities.DetectionLevel
import dev.lovelace.citovision.domain.entities.Priority
import kotlin.time.Instant

/**
 * Conversión entre el dominio [Analysis] y el documento Firestore (SPEC-0005). El `id` del análisis es
 * el id del documento; `imageUrl` (URL de descarga en Storage) se mapea a [Analysis.imagePath].
 */

internal fun analysisToFirestoreFields(
    ownerUid: String,
    analysis: Analysis,
    imageUrl: String?,
): Map<String, FirestoreValue> =
    buildMap {
        put("ownerUid", FirestoreValue(stringValue = ownerUid))
        put("patientCode", FirestoreValue(stringValue = analysis.patient))
        put("performedAt", FirestoreValue(integerValue = analysis.performedAt.toEpochMilliseconds().toString()))
        put("summary", FirestoreValue(stringValue = analysis.summary))
        put("priority", FirestoreValue(stringValue = analysis.priority.name))
        if (imageUrl != null) {
            put("imageUrl", FirestoreValue(stringValue = imageUrl))
        }
        val cellCounts =
            analysis.cellCounts.mapIndexed { index, cellCount -> cellCount.toFirestoreValue(index) }
        put("cellCounts", FirestoreValue(arrayValue = FirestoreArrayValue(values = cellCounts)))
    }

private fun CellCount.toFirestoreValue(position: Int): FirestoreValue =
    FirestoreValue(
        mapValue =
            FirestoreMapValue(
                fields =
                    mapOf(
                        "name" to FirestoreValue(stringValue = name),
                        "count" to FirestoreValue(integerValue = count.toString()),
                        "confidences" to FirestoreValue(stringValue = confidences.toConfidenceCsv()),
                        "level" to FirestoreValue(stringValue = level.name),
                        "position" to FirestoreValue(integerValue = position.toString()),
                    ),
            ),
    )

internal fun FirestoreDocument.toAnalysis(): Analysis {
    val id = name?.substringAfterLast('/').orEmpty()
    val epochMillis = fields["performedAt"]?.integerValue?.toLongOrNull() ?: 0L
    val cellCounts =
        fields["cellCounts"]
            ?.arrayValue
            ?.values
            .orEmpty()
            .map { it.mapValue?.fields.orEmpty() }
            .sortedBy { it["position"]?.integerValue?.toIntOrNull() ?: 0 }
            .map { entry ->
                CellCount(
                    name = entry["name"]?.stringValue.orEmpty(),
                    // Fallback a documentos antiguos: recuento como prefijo entero del viejo "value" ("3 (75%)").
                    count = entry["count"]?.integerValue?.toIntOrNull() ?: legacyCount(entry["value"]?.stringValue),
                    confidences = entry["confidences"]?.stringValue.toConfidenceList(),
                    // Los documentos anteriores a la política de umbrales no tienen nivel: son normales.
                    level = entry["level"]?.stringValue.toDetectionLevel(),
                )
            }
    return Analysis(
        id = id,
        patient = fields["patientCode"]?.stringValue.orEmpty(),
        performedAt = Instant.fromEpochMilliseconds(epochMillis),
        summary = fields["summary"]?.stringValue.orEmpty(),
        imagePath = fields["imageUrl"]?.stringValue,
        cellCounts = cellCounts,
        priority = Priority.fromName(fields["priority"]?.stringValue),
    )
}

private const val CONFIDENCE_SEPARATOR = ","

private fun List<Float>.toConfidenceCsv(): String = joinToString(CONFIDENCE_SEPARATOR)

private fun String?.toConfidenceList(): List<Float> =
    this?.takeIf { it.isNotEmpty() }?.split(CONFIDENCE_SEPARATOR)?.map { it.toFloat() } ?: emptyList()

private fun legacyCount(value: String?): Int = value?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0

private fun String?.toDetectionLevel(): DetectionLevel =
    DetectionLevel.entries.firstOrNull { it.name == this } ?: DetectionLevel.STANDARD
