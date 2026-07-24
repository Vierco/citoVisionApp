package dev.lovelace.citovision.presentation.events

import dev.lovelace.citovision.domain.entities.Analysis

/** Acciones de la pestaña Pacientes, resueltas en PatientsViewModel.onEvent() (SPEC-0005). */
sealed interface PatientsUiEvent {
    /** Texto del filtro del listado de códigos. */
    data class QueryChanged(
        val code: String,
    ) : PatientsUiEvent

    /** Carga (o recarga) el listado de códigos de paciente del usuario (RF-4b). */
    data object LoadCodes : PatientsUiEvent

    /** Selección de un código del listado: único camino a los resultados (RF-4c). */
    data class SelectCode(
        val code: String,
    ) : PatientsUiEvent

    /** Acción "buscar" del teclado sobre el filtro: solo abre paciente si la criba deja uno (RF-4c). */
    data object SubmitQuery : PatientsUiEvent

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

    data object DismissError : PatientsUiEvent
}
