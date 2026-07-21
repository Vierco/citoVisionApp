package dev.lovelace.citovision.domain.entities

import kotlin.time.Instant

/**
 * Análisis citológico persistido (SPEC-0004). Modelo de dominio: no contiene tipos de Room.
 *
 * [patient] es siempre un **código seudonimizado** (RN-3), nunca un nombre ni un documento de identidad.
 * [imagePath] apunta al fichero en almacenamiento privado; si es nulo o el fichero no existe, la UI muestra
 * un placeholder y se registra una advertencia: en el flujo real es una anomalía (RN-5).
 *
 * [priority] es la prioridad de revisión profesional (SPEC-0006); por defecto [Priority.BAJA] para que los
 * análisis antiguos migrados sigan siendo válidos hasta que el análisis real la calcule.
 *
 * [sampleName] es el nombre de la muestra: el nombre del fichero de imagen cargado, **sin extensión**. Es
 * nulo en los análisis antiguos migrados (que no lo guardaban); la UI muestra un rótulo genérico en ese caso.
 */
data class Analysis(
    val id: String,
    val patient: String,
    val performedAt: Instant,
    val summary: String,
    val imagePath: String?,
    val cellCounts: List<CellCount>,
    val priority: Priority = Priority.BAJA,
    val sampleName: String? = null,
)

/**
 * Entrada del conteo celular (SPEC-0006 RN-6): [count] es el recuento del tipo detectado; [confidences] es
 * la certeza del modelo por célula (0..1), presente **solo en tipos celulares reales** y vacía en las clases
 * no celulares (`Artefacto`, `Restos celulares`).
 *
 * [level] separa las detecciones normales de los **posibles hallazgos de baja confianza** (RN-2): un mismo
 * tipo con detecciones de ambos niveles produce **dos entradas**, nunca una suma (RF-2).
 */
data class CellCount(
    val name: String,
    val count: Int,
    val confidences: List<Float>,
    val level: DetectionLevel = DetectionLevel.STANDARD,
)
