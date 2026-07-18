package dev.lovelace.citovision.domain.inference

/**
 * Salida de [YoloPreprocessor]: el tensor de entrada [input] (NCHW `1×3×N×N`, RGB, normalizado 0..1) listo
 * para el modelo, y la [transform] necesaria para revertir las cajas de la salida (SPEC-0006).
 */
class PreprocessedImage(
    val input: FloatArray,
    val transform: LetterboxTransform,
)
