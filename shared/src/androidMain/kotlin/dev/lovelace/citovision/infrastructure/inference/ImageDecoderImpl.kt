package dev.lovelace.citovision.infrastructure.inference

import android.graphics.BitmapFactory
import dev.lovelace.citovision.application.ports.ImageDecoder
import dev.lovelace.citovision.domain.inference.RgbImage

/**
 * Decodifica imágenes en Android con `BitmapFactory` (SPEC-0006). Devuelve `null` si los bytes no son una
 * imagen válida. `getPixels` entrega enteros ARGB; el preprocesador ignora el canal alfa.
 */
class ImageDecoderImpl : ImageDecoder {
    override fun decode(bytes: ByteArray): RgbImage? {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        bitmap.recycle()
        return RgbImage(width = width, height = height, pixels = pixels)
    }
}
