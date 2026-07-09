package dev.lovelace.citovision.presentation.state

import dev.lovelace.citovision.domain.entities.Analysis
import org.jetbrains.compose.resources.StringResource

/**
 * Estado de la pestaña Historial (SPEC-0004). [detail] es el análisis cuyo diálogo de detalle está abierto;
 * [pendingDeletion] el que espera confirmación de borrado.
 */
data class HistoryUiState(
    val isLoading: Boolean = true,
    val analyses: List<Analysis> = emptyList(),
    val detail: Analysis? = null,
    val pendingDeletion: Analysis? = null,
    val error: StringResource? = null,
) {
    /** Estado vacío: ya cargó y no hay ningún análisis persistido (RF-2). */
    val isEmpty: Boolean get() = !isLoading && analyses.isEmpty()
}
