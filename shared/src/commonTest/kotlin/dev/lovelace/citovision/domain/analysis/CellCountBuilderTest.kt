package dev.lovelace.citovision.domain.analysis

import dev.lovelace.citovision.domain.entities.BoundingBox
import dev.lovelace.citovision.domain.entities.CellClass
import dev.lovelace.citovision.domain.entities.CellCount
import dev.lovelace.citovision.domain.entities.Detection
import dev.lovelace.citovision.domain.entities.DetectionLevel
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Conteo celular (SPEC-0006, RF-2/RN-6): recuento por tipo, confianzas por célula (orden descendente) solo
 * en tipos celulares reales, clases no celulares sin confianzas, y orden por recuento descendente y luego
 * por índice de clase.
 */
class CellCountBuilderTest {
    @Test
    fun `given cells and a non-cell when building then non-cells carry a count but no confidences`() {
        val detections =
            listOf(
                detection(CellClass.NEUTROFILO_SEGMENTADO, 0.9f),
                detection(CellClass.NEUTROFILO_SEGMENTADO, 0.8f),
                detection(CellClass.BLASTO, 0.7f),
                detection(CellClass.ARTEFACTO, 0.5f),
            )
        assertEquals(
            listOf(
                CellCount("Neutrófilo segmentado", count = 2, confidences = listOf(0.9f, 0.8f)),
                CellCount("Blasto", count = 1, confidences = listOf(0.7f)),
                CellCount("Artefacto", count = 1, confidences = emptyList()),
            ),
            CellCountBuilder.build(detections),
        )
    }

    @Test
    fun `given a type with several cells when building then its confidences are sorted descending`() {
        val detections =
            listOf(
                detection(CellClass.LINFOCITO, 0.78f),
                detection(CellClass.LINFOCITO, 0.9f),
                detection(CellClass.LINFOCITO, 0.84f),
            )
        assertEquals(
            listOf(CellCount("Linfocito", count = 3, confidences = listOf(0.9f, 0.84f, 0.78f))),
            CellCountBuilder.build(detections),
        )
    }

    @Test
    fun `given types with different counts when building then they are ordered by count descending`() {
        val detections =
            listOf(
                detection(CellClass.BLASTO, 0.9f),
                detection(CellClass.BLASTO, 0.9f),
                detection(CellClass.LINFOCITO, 0.9f),
                detection(CellClass.LINFOCITO, 0.9f),
                detection(CellClass.LINFOCITO, 0.9f),
            )
        val names = CellCountBuilder.build(detections).map { it.name }
        assertEquals(listOf("Linfocito", "Blasto"), names)
    }

    @Test
    fun `given the same type in both levels when building then they are two entries and review goes last`() {
        val detections =
            listOf(
                detection(CellClass.MIELOCITO, 0.97f),
                detection(CellClass.MIELOCITO, 0.09f).copy(level = DetectionLevel.LOW_CONFIDENCE_REVIEW),
            )
        // Nunca "Mielocito: 2": una detección al 9 % no se presenta junto a una del 97 % (RF-2).
        assertEquals(
            listOf(
                CellCount("Mielocito", count = 1, confidences = listOf(0.97f)),
                CellCount(
                    "Mielocito",
                    count = 1,
                    confidences = listOf(0.09f),
                    level = DetectionLevel.LOW_CONFIDENCE_REVIEW,
                ),
            ),
            CellCountBuilder.build(detections),
        )
    }

    private fun detection(
        cellClass: CellClass,
        confidence: Float,
    ): Detection = Detection(cellClass = cellClass, confidence = confidence, box = BoundingBox(0f, 0f, 1f, 1f))
}
