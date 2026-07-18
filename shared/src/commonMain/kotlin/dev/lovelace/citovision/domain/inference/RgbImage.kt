package dev.lovelace.citovision.domain.inference

/**
 * Imagen en píxeles RGB empaquetados (`0xRRGGBB`, el alfa se ignora), producida por el `ImageDecoder` de
 * cada plataforma (SPEC-0006) y consumida por [YoloPreprocessor]. El píxel `(x, y)` está en
 * `pixels[y * width + x]`. No es un tipo de plataforma.
 */
class RgbImage(
    val width: Int,
    val height: Int,
    val pixels: IntArray,
)
