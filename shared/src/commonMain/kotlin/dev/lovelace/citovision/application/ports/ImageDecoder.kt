package dev.lovelace.citovision.application.ports

import dev.lovelace.citovision.domain.inference.RgbImage

/**
 * Decodifica los bytes de una imagen a píxeles RGB (SPEC-0006). Implementación **por plataforma** (Android
 * `BitmapFactory`, Desktop `ImageIO`): Android carece de `javax.imageio`, por eso no puede ser común.
 * Devuelve `null` si los bytes no representan una imagen decodificable.
 */
interface ImageDecoder {
    fun decode(bytes: ByteArray): RgbImage?
}
