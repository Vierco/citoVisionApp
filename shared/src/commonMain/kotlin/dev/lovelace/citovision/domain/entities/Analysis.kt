package dev.lovelace.citovision.domain.entities

import kotlin.time.Instant

/**
 * Análisis citológico persistido (SPEC-0004). Modelo de dominio: no contiene tipos de Room.
 *
 * [patient] es siempre un **código seudonimizado** (RN-3), nunca un nombre ni un documento de identidad.
 * [imagePath] apunta al fichero en almacenamiento privado; si es nulo o el fichero no existe, la UI muestra
 * un placeholder y se registra una advertencia: en el flujo real es una anomalía (RN-5).
 */
data class Analysis(
    val id: String,
    val patient: String,
    val performedAt: Instant,
    val summary: String,
    val imagePath: String?,
    val cellCounts: List<CellCount>,
)

/** Entrada del conteo celular: el valor lleva la unidad embebida ("7.500/µL", "60%"). Ver RN-6. */
data class CellCount(
    val name: String,
    val value: String,
)
