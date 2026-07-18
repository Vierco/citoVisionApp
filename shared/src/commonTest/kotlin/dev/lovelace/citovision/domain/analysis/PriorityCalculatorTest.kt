package dev.lovelace.citovision.domain.analysis

import dev.lovelace.citovision.domain.entities.BoundingBox
import dev.lovelace.citovision.domain.entities.CellClass
import dev.lovelace.citovision.domain.entities.Detection
import dev.lovelace.citovision.domain.entities.DetectionLevel
import dev.lovelace.citovision.domain.entities.Priority
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Corazón del cribado (SPEC-0006, RN-8/RN-9): puntuación **por presencia** y umbrales 0 / 1-4 / ≥5.
 */
class PriorityCalculatorTest {
    @Test
    fun `given a blast present when calculating then priority is high`() {
        val detections = detectionsOf(CellClass.BLASTO to 1)
        assertEquals(Priority.ALTA, PriorityCalculator.priorityOf(detections))
    }

    @Test
    fun `given only a band neutrophil when calculating then priority is medium`() {
        val detections = detectionsOf(CellClass.BASTONETE to 1)
        assertEquals(Priority.MEDIA, PriorityCalculator.priorityOf(detections))
    }

    @Test
    fun `given moderate findings that add up to five when calculating then priority is high`() {
        // Mielocito (+3) + Basófilo (+2) = 5
        val detections = detectionsOf(CellClass.MIELOCITO to 1, CellClass.BASOFILO to 1)
        assertEquals(Priority.ALTA, PriorityCalculator.priorityOf(detections))
    }

    @Test
    fun `given only habitual cells when calculating then priority is low`() {
        val detections = detectionsOf(CellClass.LINFOCITO to 4, CellClass.NEUTROFILO_SEGMENTADO to 6)
        assertEquals(Priority.BAJA, PriorityCalculator.priorityOf(detections))
    }

    @Test
    fun `given only artefacts and debris when calculating then priority is low`() {
        val detections = detectionsOf(CellClass.ARTEFACTO to 3, CellClass.RESTOS_CELULARES to 2)
        assertEquals(Priority.BAJA, PriorityCalculator.priorityOf(detections))
    }

    @Test
    fun `given many band neutrophils when calculating then presence counts the weight once`() {
        // Cinco bastonetes: por recuento sumarían 5 (ALTA); por presencia suman 1 → MEDIA.
        val detections = detectionsOf(CellClass.BASTONETE to 5)
        assertEquals(Priority.MEDIA, PriorityCalculator.priorityOf(detections))
    }

    @Test
    fun `given a low confidence finding over habitual cells when calculating then priority is raised`() {
        val detections =
            detectionsOf(CellClass.LINFOCITO to 3) +
                reviewDetection(CellClass.PROMIELOCITO)
        // Sin la franja de revisión sería BAJA (linfocitos, +0); el indicio la sube un nivel.
        assertEquals(Priority.MEDIA, PriorityCalculator.priorityOf(detections))
    }

    @Test
    fun `given a low confidence finding over an already medium sample when calculating then it stays medium`() {
        // Mielocito (+3) → MEDIA. Un indicio del 9 % NO puede llevar la muestra a ALTA: esa prioridad
        // queda reservada a lo confirmado, o dos detecciones "no confirmadas" pesarían como un blasto.
        val detections = detectionsOf(CellClass.MIELOCITO to 1) + reviewDetection(CellClass.PROMIELOCITO)
        assertEquals(Priority.MEDIA, PriorityCalculator.priorityOf(detections))
    }

    @Test
    fun `given a low confidence finding over a high sample when calculating then it stays high`() {
        val detections = detectionsOf(CellClass.BLASTO to 1) + reviewDetection(CellClass.PROMIELOCITO)
        assertEquals(Priority.ALTA, PriorityCalculator.priorityOf(detections))
    }

    @Test
    fun `given a low confidence finding when calculating then it does not add its own weight`() {
        // Un blasto de baja confianza NO puntúa +5 por sí mismo: solo sube un nivel desde BAJA.
        val detections = listOf(reviewDetection(CellClass.BLASTO))
        assertEquals(Priority.MEDIA, PriorityCalculator.priorityOf(detections))
    }

    private fun detectionsOf(vararg pairs: Pair<CellClass, Int>): List<Detection> =
        pairs.flatMap { (cellClass, count) -> List(count) { detection(cellClass) } }

    private fun detection(cellClass: CellClass): Detection =
        Detection(cellClass = cellClass, confidence = 0.9f, box = BoundingBox(0f, 0f, 1f, 1f))

    private fun reviewDetection(cellClass: CellClass): Detection =
        detection(cellClass).copy(confidence = 0.09f, level = DetectionLevel.LOW_CONFIDENCE_REVIEW)
}
