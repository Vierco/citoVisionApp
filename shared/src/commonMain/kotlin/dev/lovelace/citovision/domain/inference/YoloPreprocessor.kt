package dev.lovelace.citovision.domain.inference

import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Prepara la imagen para el modelo YOLO11s-seg (SPEC-0006): **letterbox** (escala preservando el aspecto y
 * relleno gris centrado) + **normalización** a 0..1 en layout **NCHW** RGB. Es determinista y sin dependencia
 * de plataforma. El remuestreo es **bilineal** para acercarse al preprocesado de referencia de Ultralytics.
 */
object YoloPreprocessor {
    const val INPUT_SIZE = 640

    private const val CHANNELS = 3
    private const val COLOR_MAX = 255f
    private const val PAD_VALUE = 114f / COLOR_MAX
    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
    private const val BLUE_SHIFT = 0

    fun preprocess(
        image: RgbImage,
        inputSize: Int = INPUT_SIZE,
    ): PreprocessedImage {
        val scale = minOf(inputSize.toFloat() / image.width, inputSize.toFloat() / image.height)
        val scaledWidth = (image.width * scale).roundToInt()
        val scaledHeight = (image.height * scale).roundToInt()
        val padLeft = (inputSize - scaledWidth) / 2
        val padTop = (inputSize - scaledHeight) / 2

        val plane = inputSize * inputSize
        val input = FloatArray(CHANNELS * plane) { PAD_VALUE }
        for (y in 0 until scaledHeight) {
            val srcY = ((y + 0.5f) / scale - 0.5f).coerceIn(0f, (image.height - 1).toFloat())
            for (x in 0 until scaledWidth) {
                val srcX = ((x + 0.5f) / scale - 0.5f).coerceIn(0f, (image.width - 1).toFloat())
                val pixelIndex = (padTop + y) * inputSize + (padLeft + x)
                sampleInto(image, srcX, srcY, input, pixelIndex, plane)
            }
        }

        val transform =
            LetterboxTransform(
                scale = scale,
                padX = padLeft.toFloat(),
                padY = padTop.toFloat(),
                originalWidth = image.width,
                originalHeight = image.height,
                inputSize = inputSize,
            )
        return PreprocessedImage(input = input, transform = transform)
    }

    private fun sampleInto(
        image: RgbImage,
        srcX: Float,
        srcY: Float,
        input: FloatArray,
        pixelIndex: Int,
        plane: Int,
    ) {
        val x0 = floor(srcX).toInt()
        val y0 = floor(srcY).toInt()
        val x1 = (x0 + 1).coerceAtMost(image.width - 1)
        val y1 = (y0 + 1).coerceAtMost(image.height - 1)
        val dx = srcX - x0
        val dy = srcY - y0
        val p00 = image.pixels[y0 * image.width + x0]
        val p01 = image.pixels[y0 * image.width + x1]
        val p10 = image.pixels[y1 * image.width + x0]
        val p11 = image.pixels[y1 * image.width + x1]
        input[pixelIndex] = interpolate(p00, p01, p10, p11, dx, dy, RED_SHIFT)
        input[plane + pixelIndex] = interpolate(p00, p01, p10, p11, dx, dy, GREEN_SHIFT)
        input[2 * plane + pixelIndex] = interpolate(p00, p01, p10, p11, dx, dy, BLUE_SHIFT)
    }

    private fun interpolate(
        p00: Int,
        p01: Int,
        p10: Int,
        p11: Int,
        dx: Float,
        dy: Float,
        shift: Int,
    ): Float {
        val c00 = ((p00 shr shift) and 0xFF).toFloat()
        val c01 = ((p01 shr shift) and 0xFF).toFloat()
        val c10 = ((p10 shr shift) and 0xFF).toFloat()
        val c11 = ((p11 shr shift) and 0xFF).toFloat()
        val top = c00 + (c01 - c00) * dx
        val bottom = c10 + (c11 - c10) * dx
        return (top + (bottom - top) * dy) / COLOR_MAX
    }
}
