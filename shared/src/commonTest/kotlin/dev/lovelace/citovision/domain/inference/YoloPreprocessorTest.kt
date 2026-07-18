package dev.lovelace.citovision.domain.inference

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Preprocesado (SPEC-0006): letterbox + normalización 0..1 en NCHW. Se usan imágenes de color sólido para
 * que el remuestreo bilineal sea trivial y las aserciones deterministas.
 */
class YoloPreprocessorTest {
    @Test
    fun `given a square image when preprocessing then it fills the canvas without padding`() {
        val image = solidImage(width = 100, height = 100, r = 255, g = 0, b = 0)
        val result = YoloPreprocessor.preprocess(image, inputSize = 640)
        assertEquals(0f, result.transform.padX)
        assertEquals(0f, result.transform.padY)
        assertEquals(6.4, result.transform.scale.toDouble(), 0.0001)
        val plane = 640 * 640
        val center = 320 * 640 + 320
        assertEquals(1.0, result.input[center].toDouble(), 0.01)
        assertEquals(0.0, result.input[plane + center].toDouble(), 0.01)
        assertEquals(0.0, result.input[2 * plane + center].toDouble(), 0.01)
    }

    @Test
    fun `given a wide image when preprocessing then it letterboxes with vertical gray padding`() {
        val image = solidImage(width = 100, height = 50, r = 255, g = 0, b = 0)
        val result = YoloPreprocessor.preprocess(image, inputSize = 640)
        // scale = 6.4 → 640×320, padTop = 160.
        assertEquals(160f, result.transform.padY)
        assertEquals(0f, result.transform.padX)
        val plane = 640 * 640
        val paddingPixel = 10 * 640 + 320
        val imagePixel = 300 * 640 + 320
        assertEquals(114.0 / 255.0, result.input[paddingPixel].toDouble(), 0.01)
        assertEquals(1.0, result.input[imagePixel].toDouble(), 0.01)
        assertEquals(0.0, result.input[plane + imagePixel].toDouble(), 0.01)
    }

    private fun solidImage(
        width: Int,
        height: Int,
        r: Int,
        g: Int,
        b: Int,
    ): RgbImage {
        val packed = (r shl 16) or (g shl 8) or b
        return RgbImage(width = width, height = height, pixels = IntArray(width * height) { packed })
    }
}
