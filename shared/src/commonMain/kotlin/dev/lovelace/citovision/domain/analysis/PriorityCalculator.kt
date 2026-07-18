package dev.lovelace.citovision.domain.analysis

import dev.lovelace.citovision.domain.entities.Detection
import dev.lovelace.citovision.domain.entities.DetectionLevel
import dev.lovelace.citovision.domain.entities.Priority

/**
 * Calcula la [Priority] de revisión a partir de las detecciones (SPEC-0006, RN-8/RN-9). La puntuación es
 * **por presencia**: cada tipo presente aporta su peso **una sola vez**, sin importar cuántas células de ese
 * tipo haya. Umbrales: `0 → BAJA`, `1–4 → MEDIA`, `≥5 → ALTA`.
 *
 * Solo puntúan las detecciones [DetectionLevel.STANDARD]. Un **posible hallazgo de baja confianza** no suma
 * puntos —sería puntuar evidencia del 9 %— pero **saca la muestra de BAJA a MEDIA** (RN-9b): basta para que
 * no se quede la última de la cola, y **nunca alcanza ALTA**, que queda reservada a lo confirmado.
 * **No es un diagnóstico** (RN-10).
 */
object PriorityCalculator {
    private const val HIGH_THRESHOLD = 5
    private const val MEDIUM_THRESHOLD = 1

    fun priorityOf(detections: List<Detection>): Priority {
        val score =
            detections
                .filter { it.level == DetectionLevel.STANDARD }
                .map { it.cellClass }
                .distinct()
                .sumOf { it.priorityWeight }
        val priority =
            when {
                score >= HIGH_THRESHOLD -> Priority.ALTA
                score >= MEDIUM_THRESHOLD -> Priority.MEDIA
                else -> Priority.BAJA
            }
        val hasReviewFinding = detections.any { it.level == DetectionLevel.LOW_CONFIDENCE_REVIEW }
        return if (hasReviewFinding && priority == Priority.BAJA) Priority.MEDIA else priority
    }
}
