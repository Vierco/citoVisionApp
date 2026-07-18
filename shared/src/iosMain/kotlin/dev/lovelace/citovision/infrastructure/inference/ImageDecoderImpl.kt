package dev.lovelace.citovision.infrastructure.inference

import dev.lovelace.citovision.application.ports.ImageDecoder
import dev.lovelace.citovision.domain.inference.RgbImage

/**
 * Stub de iOS (SPEC-0006): la inferencia on-device en iOS queda para una fase futura (cinterop con ONNX
 * Runtime C + decodificado con CoreGraphics). Devuelve `null` para que el detector responda de forma
 * controlada con `ImageDecodeFailed` en lugar de crashear.
 */
class ImageDecoderImpl : ImageDecoder {
    override fun decode(bytes: ByteArray): RgbImage? = null
}
