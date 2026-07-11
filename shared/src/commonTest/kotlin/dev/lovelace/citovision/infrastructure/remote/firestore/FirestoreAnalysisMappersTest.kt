package dev.lovelace.citovision.infrastructure.remote.firestore

import dev.lovelace.citovision.domain.entities.Analysis
import dev.lovelace.citovision.domain.entities.CellCount
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
                        "imageUrl" to FirestoreValue(stringValue = "https://img/xyz"),
                        "cellCounts" to
                            FirestoreValue(
                                arrayValue =
                                    FirestoreArrayValue(
                                        values =
                                            listOf(
                                                mapValueOf(
                                                    "name" to FirestoreValue(stringValue = "Neutro"),
                                                    "value" to FirestoreValue(stringValue = "60%"),
                                                    "position" to FirestoreValue(integerValue = "1"),
                                                ),
                                                mapValueOf(
                                                    "name" to FirestoreValue(stringValue = "Leuco"),
                                                    "value" to FirestoreValue(stringValue = "7500"),
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
        assertEquals(
            listOf(CellCount("Leuco", "7500"), CellCount("Neutro", "60%")),
            analysis.cellCounts,
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
                cellCounts = listOf(CellCount("Leuco", "7500"), CellCount("Neutro", "60%")),
            )

        val fields = analysisToFirestoreFields(ownerUid = "u1", analysis = analysis, imageUrl = "https://img/a1")

        assertEquals("u1", fields["ownerUid"]?.stringValue)
        assertEquals("12-34", fields["patientCode"]?.stringValue)
        assertEquals("2000", fields["performedAt"]?.integerValue)
        assertEquals("https://img/a1", fields["imageUrl"]?.stringValue)
        val cellCounts = fields["cellCounts"]?.arrayValue?.values.orEmpty()
        assertEquals(2, cellCounts.size)
        assertEquals("0", cellCounts[0].mapValue?.fields?.get("position")?.integerValue)
        assertEquals("Leuco", cellCounts[0].mapValue?.fields?.get("name")?.stringValue)
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
