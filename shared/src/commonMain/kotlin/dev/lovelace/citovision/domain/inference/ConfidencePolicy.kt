package dev.lovelace.citovision.domain.inference

import dev.lovelace.citovision.domain.entities.CellClass
import dev.lovelace.citovision.domain.entities.DetectionLevel

/**
 * Política de umbrales de confianza por clase (SPEC-0006, RN-2/RN-3). Las clases **críticas** (RN-8 con
 * métricas de sensibilidad validadas: `Blasto`, `Linfocito atípico`, `Metamielocito`, `Mielocito`,
 * `Promielocito`) se aceptan desde [CRITICAL_THRESHOLD] y, entre [MINIMUM_THRESHOLD] y ese valor, se
 * conservan como **posible hallazgo pendiente de revisión** en vez de descartarse; el resto de clases
 * mantiene el umbral estándar de 0,25.
 *
 * El motivo es de producto, no de precisión: el peor error del sistema es **perder** un hallazgo relevante,
 * porque son justamente los que la herramienta existe para poner los primeros en la cola de revisión. Sobre
 * `valid`, 0,10 y 0,08 recuperan los mismos verdaderos positivos y 0,10 da menos falsos positivos, de ahí el
 * umbral principal. Una detección de baja confianza **no es un diagnóstico ni una confirmación** (RN-10).
 */
object ConfidencePolicy {
    /**
     * Suelo de lectura de la salida del modelo: el postprocesado **debe** conservar candidatos desde aquí,
     * antes del NMS. Filtrar antes por 0,25 perdería los hallazgos críticos débiles irremediablemente.
     */
    const val MINIMUM_THRESHOLD = 0.08f

    private const val STANDARD_THRESHOLD = 0.25f
    private const val CRITICAL_THRESHOLD = 0.10f

    /** Nivel con el que se acepta la detección, o `null` si queda por debajo del umbral y se descarta. */
    fun levelOf(
        cellClass: CellClass,
        confidence: Float,
    ): DetectionLevel? =
        when {
            !cellClass.isCritical ->
                if (confidence >= STANDARD_THRESHOLD) DetectionLevel.STANDARD else null

            confidence >= CRITICAL_THRESHOLD -> DetectionLevel.STANDARD
            confidence >= MINIMUM_THRESHOLD -> DetectionLevel.LOW_CONFIDENCE_REVIEW
            else -> null
        }
}
