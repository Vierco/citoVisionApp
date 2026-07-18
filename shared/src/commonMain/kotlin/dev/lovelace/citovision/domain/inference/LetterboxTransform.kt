package dev.lovelace.citovision.domain.inference

import dev.lovelace.citovision.domain.entities.BoundingBox

/**
 * Parámetros del letterbox aplicado por [YoloPreprocessor]: la imagen original se escala por [scale] y se
 * centra en el lienzo de entrada con un relleno de [padX]/[padY] píxeles. Permite a [YoloPostprocessor]
 * **revertir** las cajas del espacio de entrada (640×640) al de la imagen original y normalizarlas a 0..1.
 */
data class LetterboxTransform(
    val scale: Float,
    val padX: Float,
    val padY: Float,
    val originalWidth: Int,
    val originalHeight: Int,
    val inputSize: Int,
) {
    /**
     * Convierte una caja `xyxy` en coordenadas del lienzo de entrada a una [BoundingBox] normalizada 0..1
     * respecto a la **imagen original** (deshace escala y padding, y recorta a los límites de la imagen).
     */
    fun toOriginalBox(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ): BoundingBox =
        BoundingBox(
            left = mapX(left),
            top = mapY(top),
            right = mapX(right),
            bottom = mapY(bottom),
        )

    private fun mapX(value: Float): Float =
        ((value - padX) / scale).coerceIn(0f, originalWidth.toFloat()) / originalWidth

    private fun mapY(value: Float): Float =
        ((value - padY) / scale).coerceIn(0f, originalHeight.toFloat()) / originalHeight
}
