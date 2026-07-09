package dev.lovelace.citovision.presentation.events

import dev.lovelace.citovision.domain.entities.Analysis

/** Acciones de usuario de la pestaña Historial, resueltas en HistoryViewModel.onEvent() (SPEC-0004). */
sealed interface HistoryUiEvent {
    data class ShowDetail(
        val analysis: Analysis,
    ) : HistoryUiEvent

    data object DismissDetail : HistoryUiEvent

    /** Pulsación larga sobre una card: pide confirmación antes de borrar (RF-5). */
    data class RequestDelete(
        val analysis: Analysis,
    ) : HistoryUiEvent

    data object ConfirmDelete : HistoryUiEvent

    data object CancelDelete : HistoryUiEvent

    data object DismissError : HistoryUiEvent
}
