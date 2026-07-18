package dev.lovelace.citovision.domain.analysis

import dev.lovelace.citovision.domain.entities.CellClass
import dev.lovelace.citovision.domain.entities.Detection

/**
 * Recuento por tipo derivado de las detecciones (SPEC-0006). [counts] solo contiene los tipos presentes;
 * [totalCells] es la **suma de instancias de clases celulares reales** (RN-6), usada como "N células
 * detectadas" del resumen, de modo que `Artefacto` y `Restos celulares` no cuentan como célula.
 */
data class CellTally(
    val counts: Map<CellClass, Int>,
    val totalCells: Int,
) {
    companion object {
        fun of(detections: List<Detection>): CellTally {
            val counts = detections.groupingBy { it.cellClass }.eachCount()
            val totalCells = counts.filterKeys { it.isCell }.values.sum()
            return CellTally(counts = counts, totalCells = totalCells)
        }
    }
}
