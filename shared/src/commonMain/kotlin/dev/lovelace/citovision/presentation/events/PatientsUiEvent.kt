package dev.lovelace.citovision.presentation.events

import dev.lovelace.citovision.domain.entities.Analysis

/** Acciones de la pestaña Pacientes, resueltas en PatientsViewModel.onEvent() (SPEC-0005). */
sealed interface PatientsUiEvent {
    data class QueryChanged(
        val code: String,
    ) : PatientsUiEvent

    data object Search : PatientsUiEvent

    data object NewSearch : PatientsUiEvent

    /** Recargar los análisis del paciente mostrado desde la BD remota. */
    data object Refresh : PatientsUiEvent

    data class ShowDetail(
        val analysis: Analysis,
    ) : PatientsUiEvent

    data object DismissDetail : PatientsUiEvent

    /** Pulsación larga sobre una card: pide confirmación para borrar en remoto (RF-9). */
    data class RequestDelete(
        val analysis: Analysis,
    ) : PatientsUiEvent

    data object ConfirmDelete : PatientsUiEvent

    data object CancelDelete : PatientsUiEvent

    data object DismissNoResults : PatientsUiEvent

    data object DismissRequiresAccount : PatientsUiEvent

    data object DismissError : PatientsUiEvent
}
