package dev.lovelace.citovision.domain.analysis

import dev.lovelace.citovision.domain.entities.CellClass
import dev.lovelace.citovision.domain.entities.CellCount
import dev.lovelace.citovision.domain.entities.Detection
import dev.lovelace.citovision.domain.entities.DetectionLevel

/**
 * Construye el conteo celular (`List<CellCount>`) a partir de las detecciones (SPEC-0006, RF-2). Se agrupa
 * por **tipo y nivel**: un mismo tipo detectado como normal y como posible hallazgo de baja confianza da
 * **dos entradas**, para no presentar como confirmado lo que no lo está (RN-3). Cada entrada lleva su
 * recuento y, **solo para tipos celulares reales**, la lista de confianzas por célula ordenada de mayor a
 * menor (las clases no celulares quedan sin confianzas, RN-6).
 *
 * Orden del resultado: primero todo lo `STANDARD` —células reales y, al final, las no celulares
 * (Artefacto, Restos)— y después los hallazgos de baja confianza; dentro de cada grupo, por recuento
 * descendente y, a igualdad, por índice de clase.
 */
object CellCountBuilder {
    fun build(detections: List<Detection>): List<CellCount> =
        detections
            .groupBy { it.cellClass to it.level }
            .entries
            .sortedWith(
                compareBy<Map.Entry<Pair<CellClass, DetectionLevel>, List<Detection>>> { it.key.second }
                    .thenByDescending { it.key.first.isCell }
                    .thenByDescending { it.value.size }
                    .thenBy { it.key.first.index },
            ).map { (key, group) ->
                val (cellClass, level) = key
                val confidences =
                    if (cellClass.isCell) group.map { it.confidence }.sortedDescending() else emptyList()
                CellCount(
                    name = cellClass.label,
                    count = group.size,
                    confidences = confidences,
                    level = level,
                )
            }
}
