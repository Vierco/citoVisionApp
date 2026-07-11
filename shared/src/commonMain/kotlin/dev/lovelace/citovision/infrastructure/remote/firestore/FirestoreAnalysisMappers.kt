package dev.lovelace.citovision.infrastructure.remote.firestore

import dev.lovelace.citovision.domain.entities.Analysis
import dev.lovelace.citovision.domain.entities.CellCount
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
        if (imageUrl != null) {
            put("imageUrl", FirestoreValue(stringValue = imageUrl))
        }
        put(
            "cellCounts",
            FirestoreValue(
                arrayValue =
                    FirestoreArrayValue(
                        values =
                            analysis.cellCounts.mapIndexed { index, cellCount ->
                                FirestoreValue(
                                    mapValue =
                                        FirestoreMapValue(
                                            fields =
                                                mapOf(
                                                    "name" to FirestoreValue(stringValue = cellCount.name),
                                                    "value" to FirestoreValue(stringValue = cellCount.value),
                                                    "position" to FirestoreValue(integerValue = index.toString()),
                                                ),
                                        ),
                                )
                            },
                    ),
            ),
        )
    }

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
                    value = entry["value"]?.stringValue.orEmpty(),
                )
            }
    return Analysis(
        id = id,
        patient = fields["patientCode"]?.stringValue.orEmpty(),
        performedAt = Instant.fromEpochMilliseconds(epochMillis),
        summary = fields["summary"]?.stringValue.orEmpty(),
        imagePath = fields["imageUrl"]?.stringValue,
        cellCounts = cellCounts,
    )
}
