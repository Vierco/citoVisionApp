package dev.lovelace.citovision.domain.inference

import dev.lovelace.citovision.domain.entities.CellClass
import dev.lovelace.citovision.domain.entities.Detection
import dev.lovelace.citovision.domain.entities.DetectionLevel
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Postprocesado (SPEC-0006): argmax de clase, umbral de confianza, mapeo de cajas a la imagen original y NMS
 * por clase. Se construyen salidas sintéticas con `atributos = 4 + 14` (rama de detección, sin máscaras).
 */
class YoloPostprocessorTest {
    private val identityTransform =
        LetterboxTransform(
            scale = 1f,
            padX = 0f,
            padY = 0f,
            originalWidth = 100,
            originalHeight = 100,
            inputSize = 100,
        )

    @Test
    fun `given anchors above and below threshold when decoding then only strong detections survive`() {
        val anchors = 2
        val output = FloatArray(ATTRIBUTES * anchors)

        fun set(
            channel: Int,
            anchor: Int,
            value: Float,
        ) {
            output[channel * anchors + anchor] = value
        }
        // Anchor 0: blasto fuerte, caja centrada 20×20 en (50, 50).
        set(0, 0, 50f)
        set(1, 0, 50f)
        set(2, 0, 20f)
        set(3, 0, 20f)
        set(BOX_CHANNELS + CellClass.BLASTO.index, 0, 0.9f)
        // Anchor 1: todas las clases por debajo del umbral.
        set(0, 1, 10f)
        set(1, 1, 10f)
        set(2, 1, 4f)
        set(3, 1, 4f)
        for (c in 0 until CellClass.entries.size) {
            set(BOX_CHANNELS + c, 1, 0.1f)
        }

        val detections =
            YoloPostprocessor.decode(
                output = output,
                attributes = ATTRIBUTES,
                transform = identityTransform,
                iouThreshold = 0.5f,
            )

        assertEquals(1, detections.size)
        val detection = detections.first()
        assertEquals(CellClass.BLASTO, detection.cellClass)
        assertEquals(0.9, detection.confidence.toDouble(), 0.0001)
        assertEquals(0.4, detection.box.left.toDouble(), 0.001)
        assertEquals(0.4, detection.box.top.toDouble(), 0.001)
        assertEquals(0.6, detection.box.right.toDouble(), 0.001)
        assertEquals(0.6, detection.box.bottom.toDouble(), 0.001)
    }

    @Test
    fun `given two overlapping boxes of the same class when decoding then NMS keeps the strongest`() {
        val detections = decodeTwoBoxes(CellClass.BLASTO, CellClass.BLASTO)
        assertEquals(1, detections.size)
        assertEquals(0.9, detections.first().confidence.toDouble(), 0.0001)
    }

    @Test
    fun `given two overlapping boxes of different classes when decoding then NMS keeps both`() {
        val detections = decodeTwoBoxes(CellClass.BLASTO, CellClass.LINFOCITO)
        assertEquals(2, detections.size)
    }

    @Test
    fun `given a weak critical box overlapping a strong one of another class when decoding then both survive`() {
        // El NMS es por clase: un hallazgo débil no debe desaparecer por solaparse con una detección
        // fuerte de otro tipo, que es justo el caso de img_001304 (Promielocito 0.0897 sobre Mielocito).
        val detections =
            decodeTwoBoxes(
                first = CellClass.MIELOCITO,
                second = CellClass.PROMIELOCITO,
                secondScore = 0.0897f,
            )

        assertEquals(2, detections.size)
        assertEquals(
            listOf(DetectionLevel.STANDARD, DetectionLevel.LOW_CONFIDENCE_REVIEW),
            detections.sortedByDescending { it.confidence }.map { it.level },
        )
    }

    private fun decodeTwoBoxes(
        first: CellClass,
        second: CellClass,
        secondScore: Float = 0.6f,
    ): List<Detection> {
        val anchors = 2
        val output = FloatArray(ATTRIBUTES * anchors)

        fun set(
            channel: Int,
            anchor: Int,
            value: Float,
        ) {
            output[channel * anchors + anchor] = value
        }
        // Dos cajas muy solapadas: (50,50,20,20) y (52,52,20,20).
        set(0, 0, 50f)
        set(1, 0, 50f)
        set(2, 0, 20f)
        set(3, 0, 20f)
        set(BOX_CHANNELS + first.index, 0, 0.9f)
        set(0, 1, 52f)
        set(1, 1, 52f)
        set(2, 1, 20f)
        set(3, 1, 20f)
        set(BOX_CHANNELS + second.index, 1, secondScore)
        return YoloPostprocessor.decode(
            output = output,
            attributes = ATTRIBUTES,
            transform = identityTransform,
            iouThreshold = 0.5f,
        )
    }

    @Test
    fun `given a critical class between the review thresholds when decoding then it survives for review`() {
        val detections = decodeSingleBox(CellClass.PROMIELOCITO, score = 0.0897f)

        assertEquals(1, detections.size)
        assertEquals(CellClass.PROMIELOCITO, detections.first().cellClass)
        assertEquals(DetectionLevel.LOW_CONFIDENCE_REVIEW, detections.first().level)
    }

    @Test
    fun `given a critical class above its lowered threshold when decoding then it is a standard detection`() {
        val detections = decodeSingleBox(CellClass.BLASTO, score = 0.12f)

        assertEquals(DetectionLevel.STANDARD, detections.single().level)
    }

    @Test
    fun `given a non critical class with the same weak score when decoding then it is discarded`() {
        val detections = decodeSingleBox(CellClass.NEUTROFILO_SEGMENTADO, score = 0.0897f)

        assertEquals(0, detections.size)
    }

    private fun decodeSingleBox(
        cellClass: CellClass,
        score: Float,
    ): List<Detection> {
        val anchors = 1
        val output = FloatArray(ATTRIBUTES * anchors)
        output[0] = 50f
        output[1] = 50f
        output[2] = 20f
        output[3] = 20f
        output[BOX_CHANNELS + cellClass.index] = score
        return YoloPostprocessor.decode(
            output = output,
            attributes = ATTRIBUTES,
            transform = identityTransform,
            iouThreshold = 0.5f,
        )
    }

    private companion object {
        const val BOX_CHANNELS = 4
        const val ATTRIBUTES = BOX_CHANNELS + 14
    }
}
