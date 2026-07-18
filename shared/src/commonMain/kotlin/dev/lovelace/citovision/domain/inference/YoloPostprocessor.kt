package dev.lovelace.citovision.domain.inference

import dev.lovelace.citovision.domain.entities.BoundingBox
import dev.lovelace.citovision.domain.entities.CellClass
import dev.lovelace.citovision.domain.entities.Detection
import kotlin.math.max
import kotlin.math.min

/**
 * Decodifica la salida de detección del modelo YOLO11s-seg (SPEC-0006). Se usa **solo la rama de detección**
 * `[1, atributos, anclas]` (caja `xywh` + puntuaciones de clase); los coeficientes/prototipos de máscara se
 * ignoran. La salida se asume con layout atributos-mayor (por defecto de Ultralytics).
 *
 * El orden importa (RN-2): se conservan los candidatos desde [ConfidencePolicy.MINIMUM_THRESHOLD], se mapean
 * las cajas a la imagen original ([LetterboxTransform]), se aplica **NMS por clase** y **solo entonces** se
 * asigna el nivel de cada detección. Filtrar antes por el umbral estándar descartaría los hallazgos críticos
 * débiles antes de poder clasificarlos.
 */
object YoloPostprocessor {
    private const val BOX_CHANNELS = 4

    fun decode(
        output: FloatArray,
        attributes: Int,
        transform: LetterboxTransform,
        iouThreshold: Float,
    ): List<Detection> {
        val anchors = output.size / attributes
        val candidates = collectCandidates(output, anchors, transform)
        return nonMaxSuppression(candidates, iouThreshold).mapNotNull(::withLevel)
    }

    private fun withLevel(detection: Detection): Detection? =
        ConfidencePolicy
            .levelOf(detection.cellClass, detection.confidence)
            ?.let { detection.copy(level = it) }

    private fun collectCandidates(
        output: FloatArray,
        anchors: Int,
        transform: LetterboxTransform,
    ): List<Detection> {
        val numClasses = CellClass.entries.size
        val candidates = mutableListOf<Detection>()
        for (anchor in 0 until anchors) {
            var bestClass = -1
            var bestScore = 0f
            for (c in 0 until numClasses) {
                val score = output[(BOX_CHANNELS + c) * anchors + anchor]
                if (score > bestScore) {
                    bestScore = score
                    bestClass = c
                }
            }
            if (bestClass < 0 || bestScore < ConfidencePolicy.MINIMUM_THRESHOLD) continue
            val cellClass = CellClass.fromIndex(bestClass) ?: continue
            val centerX = output[anchor]
            val centerY = output[anchors + anchor]
            val width = output[2 * anchors + anchor]
            val height = output[3 * anchors + anchor]
            val box =
                transform.toOriginalBox(
                    left = centerX - width / 2f,
                    top = centerY - height / 2f,
                    right = centerX + width / 2f,
                    bottom = centerY + height / 2f,
                )
            candidates += Detection(cellClass = cellClass, confidence = bestScore, box = box)
        }
        return candidates
    }

    private fun nonMaxSuppression(
        candidates: List<Detection>,
        iouThreshold: Float,
    ): List<Detection> {
        val kept = mutableListOf<Detection>()
        for ((_, group) in candidates.groupBy { it.cellClass }) {
            val remaining = group.sortedByDescending { it.confidence }.toMutableList()
            while (remaining.isNotEmpty()) {
                val best = remaining.removeAt(0)
                kept += best
                remaining.removeAll { intersectionOverUnion(best.box, it.box) > iouThreshold }
            }
        }
        return kept
    }

    private fun intersectionOverUnion(
        a: BoundingBox,
        b: BoundingBox,
    ): Float {
        val interLeft = max(a.left, b.left)
        val interTop = max(a.top, b.top)
        val interRight = min(a.right, b.right)
        val interBottom = min(a.bottom, b.bottom)
        val interWidth = (interRight - interLeft).coerceAtLeast(0f)
        val interHeight = (interBottom - interTop).coerceAtLeast(0f)
        val intersection = interWidth * interHeight
        val union = area(a) + area(b) - intersection
        return if (union <= 0f) 0f else intersection / union
    }

    private fun area(box: BoundingBox): Float = (box.right - box.left) * (box.bottom - box.top)
}
