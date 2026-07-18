package dev.lovelace.citovision.infrastructure.remote.firestore

import dev.lovelace.citovision.domain.entities.Analysis
import dev.lovelace.citovision.domain.entities.CellCount
import dev.lovelace.citovision.domain.entities.DetectionLevel
import dev.lovelace.citovision.domain.entities.Priority
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class FirestoreAnalysisMappersTest {
    private fun mapValueOf(vararg pairs: Pair<String, FirestoreValue>) =
        FirestoreValue(mapValue = FirestoreMapValue(fields = mapOf(*pairs)))

    @Test
    fun `given a firestore document when mapping to domain then reads fields and sorts cell counts`() {
        val document =
            FirestoreDocument(
                name = "projects/p/databases/(default)/documents/analyses/xyz",
                fields =
                    mapOf(
                        "patientCode" to FirestoreValue(stringValue = "12-34"),
                        "performedAt" to FirestoreValue(integerValue = "1700"),
                        "summary" to FirestoreValue(stringValue = "resumen"),
                        "priority" to FirestoreValue(stringValue = "ALTA"),
                        "imageUrl" to FirestoreValue(stringValue = "https://img/xyz"),
                        "cellCounts" to
                            FirestoreValue(
                                arrayValue =
                                    FirestoreArrayValue(
                                        values =
                                            listOf(
                                                mapValueOf(
                                                    "name" to FirestoreValue(stringValue = "Neutro"),
                                                    "count" to FirestoreValue(integerValue = "1"),
                                                    "confidences" to FirestoreValue(stringValue = "0.85"),
                                                    "position" to FirestoreValue(integerValue = "1"),
                                                ),
                                                mapValueOf(
                                                    "name" to FirestoreValue(stringValue = "Leuco"),
                                                    "count" to FirestoreValue(integerValue = "3"),
                                                    "confidences" to FirestoreValue(stringValue = "0.9,0.8,0.7"),
                                                    "position" to FirestoreValue(integerValue = "0"),
                                                ),
                                            ),
                                    ),
                            ),
                    ),
            )

        val analysis = document.toAnalysis()

        assertEquals("xyz", analysis.id)
        assertEquals("12-34", analysis.patient)
        assertEquals(Instant.fromEpochMilliseconds(1700), analysis.performedAt)
        assertEquals("https://img/xyz", analysis.imagePath)
        assertEquals(Priority.ALTA, analysis.priority)
        assertEquals(
            listOf(
                CellCount("Leuco", count = 3, confidences = listOf(0.9f, 0.8f, 0.7f)),
                CellCount("Neutro", count = 1, confidences = listOf(0.85f)),
            ),
            analysis.cellCounts,
        )
    }

    @Test
    fun `given a legacy document with value strings when mapping then count falls back to the value prefix`() {
        val document =
            FirestoreDocument(
                name = "projects/p/databases/(default)/documents/analyses/old",
                fields =
                    mapOf(
                        "cellCounts" to
                            FirestoreValue(
                                arrayValue =
                                    FirestoreArrayValue(
                                        values =
                                            listOf(
                                                mapValueOf(
                                                    "name" to FirestoreValue(stringValue = "Linfocito"),
                                                    "value" to FirestoreValue(stringValue = "7 (58%)"),
                                                    "position" to FirestoreValue(integerValue = "0"),
                                                ),
                                            ),
                                    ),
                            ),
                    ),
            )

        assertEquals(
            listOf(CellCount("Linfocito", count = 7, confidences = emptyList())),
            document.toAnalysis().cellCounts,
        )
    }

    @Test
    fun `given a document without image when mapping to domain then imagePath is null`() {
        val document =
            FirestoreDocument(
                name = "projects/p/databases/(default)/documents/analyses/x",
                fields = mapOf("patientCode" to FirestoreValue(stringValue = "1-2")),
            )

        assertNull(document.toAnalysis().imagePath)
        assertEquals(Priority.BAJA, document.toAnalysis().priority)
    }

    @Test
    fun `given a domain analysis when mapping to fields then uses typed values and positions`() {
        val analysis =
            Analysis(
                id = "a1",
                patient = "12-34",
                performedAt = Instant.fromEpochMilliseconds(2000),
                summary = "resumen",
                imagePath = "/local/a1.png",
                cellCounts =
                    listOf(
                        CellCount("Leuco", count = 3, confidences = listOf(0.9f, 0.8f, 0.7f)),
                        CellCount("Neutro", count = 1, confidences = emptyList()),
                    ),
                priority = Priority.ALTA,
            )

        val fields = analysisToFirestoreFields(ownerUid = "u1", analysis = analysis, imageUrl = "https://img/a1")

        assertEquals("u1", fields["ownerUid"]?.stringValue)
        assertEquals("12-34", fields["patientCode"]?.stringValue)
        assertEquals("2000", fields["performedAt"]?.integerValue)
        assertEquals("ALTA", fields["priority"]?.stringValue)
        assertEquals("https://img/a1", fields["imageUrl"]?.stringValue)
        val cellCounts = fields["cellCounts"]?.arrayValue?.values.orEmpty()
        assertEquals(2, cellCounts.size)
        val first = cellCounts[0].mapValue?.fields
        assertEquals("0", first?.get("position")?.integerValue)
        assertEquals("Leuco", first?.get("name")?.stringValue)
        assertEquals("3", first?.get("count")?.integerValue)
        assertEquals("0.9,0.8,0.7", first?.get("confidences")?.stringValue)
        assertEquals("STANDARD", first?.get("level")?.stringValue)
    }

    @Test
    fun `given a low confidence finding when mapping in both directions then the level round-trips`() {
        val cellCount =
            CellCount(
                "Promielocito",
                count = 1,
                confidences = listOf(0.09f),
                level = DetectionLevel.LOW_CONFIDENCE_REVIEW,
            )
        val analysis =
            Analysis(
                id = "a1",
                patient = "12-34",
                performedAt = Instant.fromEpochMilliseconds(2000),
                summary = "resumen",
                imagePath = null,
                cellCounts = listOf(cellCount),
            )

        val fields = analysisToFirestoreFields(ownerUid = "u1", analysis = analysis, imageUrl = null)
        val document =
            FirestoreDocument(
                name = "projects/p/databases/(default)/documents/analyses/a1",
                fields = fields,
            )

        assertEquals(listOf(cellCount), document.toAnalysis().cellCounts)
    }

    @Test
    fun `given a domain analysis without image when mapping to fields then omits imageUrl`() {
        val analysis =
            Analysis(
                id = "a1",
                patient = "12-34",
                performedAt = Instant.fromEpochMilliseconds(2000),
                summary = "resumen",
                imagePath = null,
                cellCounts = emptyList(),
            )

        val fields = analysisToFirestoreFields(ownerUid = "u1", analysis = analysis, imageUrl = null)

        assertNull(fields["imageUrl"])
    }
}
