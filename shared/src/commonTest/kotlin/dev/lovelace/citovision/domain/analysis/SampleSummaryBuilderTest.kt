package dev.lovelace.citovision.domain.analysis

import dev.lovelace.citovision.domain.entities.BoundingBox
import dev.lovelace.citovision.domain.entities.CellClass
import dev.lovelace.citovision.domain.entities.Detection
import dev.lovelace.citovision.domain.entities.DetectionLevel
import dev.lovelace.citovision.domain.entities.Priority
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Resumen descriptivo (SPEC-0006, RF-3): agrupa por relevancia clínica (peso > 0) y omite secciones vacías.
 */
class SampleSummaryBuilderTest {
    @Test
    fun `given relevant and habitual findings when building then it groups by clinical relevance`() {
        val detections = detectionsOf(CellClass.BLASTO to 2, CellClass.MIELOCITO to 3, CellClass.LINFOCITO to 7)
        assertEquals(
            "12 células detectadas. Prioridad de revisión: ALTA. " +
                "Hallazgos principalmente relevantes: Mielocito 3, Blasto 2. " +
                "Otros hallazgos encontrados en la revisión: Linfocito 7.",
            SampleSummaryBuilder.build(detections, Priority.ALTA),
        )
    }

    @Test
    fun `given only habitual cells when building then the relevant section is omitted`() {
        val detections = detectionsOf(CellClass.LINFOCITO to 5)
        assertEquals(
            "5 células detectadas. Prioridad de revisión: BAJA. " +
                "Otros hallazgos encontrados en la revisión: Linfocito 5.",
            SampleSummaryBuilder.build(detections, Priority.BAJA),
        )
    }

    @Test
    fun `given low confidence findings when building then they go apart and out of the cell count`() {
        val detections =
            detectionsOf(CellClass.MIELOCITO to 1, CellClass.NEUTROFILO_SEGMENTADO to 1) +
                detection(CellClass.MIELOCITO).copy(level = DetectionLevel.LOW_CONFIDENCE_REVIEW) +
                detection(CellClass.PROMIELOCITO).copy(level = DetectionLevel.LOW_CONFIDENCE_REVIEW)
        assertEquals(
            // 2 células, no 4: los indicios débiles no se cuentan como células detectadas.
            "2 células detectadas. Prioridad de revisión: ALTA. " +
                "Hallazgos principalmente relevantes: Mielocito 1. " +
                "Otros hallazgos encontrados en la revisión: Neutrófilo segmentado 1. " +
                "Posibles hallazgos de baja confianza, pendientes de revisión humana: " +
                "Mielocito 1, Promielocito 1.",
            SampleSummaryBuilder.build(detections, Priority.ALTA),
        )
    }

    private fun detectionsOf(vararg pairs: Pair<CellClass, Int>): List<Detection> =
        pairs.flatMap { (cellClass, count) -> List(count) { detection(cellClass) } }

    private fun detection(cellClass: CellClass): Detection =
        Detection(cellClass = cellClass, confidence = 0.9f, box = BoundingBox(0f, 0f, 1f, 1f))
}
