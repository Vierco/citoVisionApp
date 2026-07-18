package dev.lovelace.citovision.domain.entities

/**
 * Una célula detectada por el modelo (SPEC-0006): su [cellClass], la [confidence] (0..1), su [box] y el
 * [level] de confianza con el que se acepta (RN-2). La [box] se conserva para la futura visualización de
 * cajas/máscaras (fuera de alcance en SPEC-0006).
 */
data class Detection(
    val cellClass: CellClass,
    val confidence: Float,
    val box: BoundingBox,
    val level: DetectionLevel = DetectionLevel.STANDARD,
)

/**
 * Nivel de aceptación de una detección según la política de umbrales por clase (SPEC-0006, RN-2):
 * [STANDARD] es una detección normal y [LOW_CONFIDENCE_REVIEW] un **posible hallazgo crítico sin
 * confirmar**, que se muestra por separado y **nunca se suma al recuento normal**. Las predicciones por
 * debajo del umbral mínimo no llegan a ser una [Detection]: se descartan en el postprocesado (RN-3).
 */
enum class DetectionLevel {
    STANDARD,
    LOW_CONFIDENCE_REVIEW,
}

/** Caja contenedora en coordenadas normalizadas 0..1 (esquina superior-izquierda a inferior-derecha). */
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)
