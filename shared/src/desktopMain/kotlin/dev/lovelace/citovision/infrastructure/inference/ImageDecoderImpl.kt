package dev.lovelace.citovision.infrastructure.inference

import dev.lovelace.citovision.application.ports.ImageDecoder
import dev.lovelace.citovision.domain.inference.RgbImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

/**
 * Decodifica imágenes en Desktop (JVM) con `ImageIO` (SPEC-0006). Devuelve `null` si los bytes no son una
 * imagen decodificable. `getRGB` entrega enteros ARGB; el preprocesador ignora el canal alfa.
 */
class ImageDecoderImpl : ImageDecoder {
    override fun decode(bytes: ByteArray): RgbImage? {
        val image = ByteArrayInputStream(bytes).use { ImageIO.read(it) } ?: return null
        val width = image.width
        val height = image.height
        val pixels = IntArray(width * height)
        image.getRGB(0, 0, width, height, pixels, 0, width)
        return RgbImage(width = width, height = height, pixels = pixels)
    }
}
