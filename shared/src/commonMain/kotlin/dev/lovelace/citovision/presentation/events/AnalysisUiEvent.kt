package dev.lovelace.citovision.presentation.events

/** Acciones de usuario de la pestaña Análisis, resueltas en AnalysisViewModel.onEvent() (SPEC-0003). */
sealed interface AnalysisUiEvent {
    /** Abrir el selector de imagen. */
    data object SelectImage : AnalysisUiEvent

    /** Quitar la imagen seleccionada y volver al estado vacío (RF-6). */
    data object RemoveImage : AnalysisUiEvent

    /** Cerrar el mensaje de error. */
    data object DismissError : AnalysisUiEvent
}
