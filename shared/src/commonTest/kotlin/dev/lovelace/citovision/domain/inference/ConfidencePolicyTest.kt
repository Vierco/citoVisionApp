package dev.lovelace.citovision.domain.inference

import dev.lovelace.citovision.domain.entities.CellClass
import dev.lovelace.citovision.domain.entities.DetectionLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Política de umbrales por clase (SPEC-0006, RN-2). Se cubren los tres límites que la definen: 0,25 para las
 * clases no críticas, 0,10 para las críticas y la franja 0,08–0,10 de posible hallazgo pendiente de revisión.
 */
class ConfidencePolicyTest {
    @Test
    fun `given a non critical class below the standard threshold when evaluated then it is discarded`() {
        assertNull(ConfidencePolicy.levelOf(CellClass.NEUTROFILO_SEGMENTADO, confidence = 0.24f))
    }

    @Test
    fun `given a non critical class at the standard threshold when evaluated then it is standard`() {
        assertEquals(
            DetectionLevel.STANDARD,
            ConfidencePolicy.levelOf(CellClass.NEUTROFILO_SEGMENTADO, confidence = 0.25f),
        )
    }

    @Test
    fun `given a critical class at the critical threshold when evaluated then it is standard`() {
        assertEquals(
            DetectionLevel.STANDARD,
            ConfidencePolicy.levelOf(CellClass.MIELOCITO, confidence = 0.10f),
        )
    }

    @Test
    fun `given a critical class inside the review band when evaluated then it requires review`() {
        assertEquals(
            DetectionLevel.LOW_CONFIDENCE_REVIEW,
            ConfidencePolicy.levelOf(CellClass.PROMIELOCITO, confidence = 0.0897f),
        )
    }

    @Test
    fun `given a critical class below the review threshold when evaluated then it is discarded`() {
        assertNull(ConfidencePolicy.levelOf(CellClass.BLASTO, confidence = 0.079f))
    }

    @Test
    fun `given a weighted but non critical class when evaluated then it keeps the standard threshold`() {
        // Basófilo, Eritroblasto y Bastonete puntúan en RN-8 pero no tienen sensibilidad validada: el
        // umbral rebajado no se les aplica (ver CellClass.isCritical).
        assertNull(ConfidencePolicy.levelOf(CellClass.ERITROBLASTO, confidence = 0.12f))
        assertNull(ConfidencePolicy.levelOf(CellClass.BASTONETE, confidence = 0.12f))
    }
}
