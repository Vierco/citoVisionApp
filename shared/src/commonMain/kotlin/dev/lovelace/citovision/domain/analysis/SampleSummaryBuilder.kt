package dev.lovelace.citovision.domain.analysis

import dev.lovelace.citovision.domain.entities.CellClass
import dev.lovelace.citovision.domain.entities.Detection
import dev.lovelace.citovision.domain.entities.DetectionLevel
import dev.lovelace.citovision.domain.entities.Priority

/**
 * Genera el resumen textual descriptivo del análisis (SPEC-0006, RF-3), **sin lenguaje diagnóstico**. Separa
 * los hallazgos con peso morfológico (`> 0`, agrupación por relevancia clínica) del resto de tipos celulares
 * y clases no celulares. Es **contenido generado y persistido**, no un texto de interfaz.
 *
 * Los **posibles hallazgos de baja confianza** (RN-2) van en una frase aparte y **no entran en el recuento**
 * de células detectadas: son indicios pendientes de revisión humana, no células contadas.
 */
object SampleSummaryBuilder {
    fun build(
        detections: List<Detection>,
        priority: Priority,
    ): String {
        val standard = detections.filter { it.level == DetectionLevel.STANDARD }
        val review = detections.filter { it.level == DetectionLevel.LOW_CONFIDENCE_REVIEW }
        val tally = CellTally.of(standard)
        val reviewTally = CellTally.of(review)
        val relevant = tally.presentSortedWhere { it.priorityWeight > 0 }
        val others = tally.presentSortedWhere { it.priorityWeight == 0 }
        return buildString {
            append("${tally.totalCells} células detectadas. ")
            append("Prioridad de revisión: ${priority.label}.")
            if (relevant.isNotEmpty()) {
                val list = relevant.joinToString { entry(it, tally) }
                append(" Hallazgos principalmente relevantes: $list.")
            }
            if (others.isNotEmpty()) {
                val list = others.joinToString { entry(it, tally) }
                append(" Otros hallazgos encontrados en la revisión: $list.")
            }
            if (reviewTally.counts.isNotEmpty()) {
                val list = reviewTally.presentSortedWhere { true }.joinToString { entry(it, reviewTally) }
                append(" Posibles hallazgos de baja confianza, pendientes de revisión humana: $list.")
            }
        }
    }

    private fun CellTally.presentSortedWhere(predicate: (CellClass) -> Boolean): List<CellClass> =
        counts.keys
            .filter(predicate)
            .sortedWith(compareByDescending<CellClass> { counts.getValue(it) }.thenBy { it.index })

    private fun entry(
        cellClass: CellClass,
        tally: CellTally,
    ): String = "${cellClass.label} ${tally.counts.getValue(cellClass)}"
}
