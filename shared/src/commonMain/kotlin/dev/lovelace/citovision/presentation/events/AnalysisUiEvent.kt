package dev.lovelace.citovision.presentation.events

/** Acciones de usuario de la pestaña Análisis, resueltas en AnalysisViewModel.onEvent() (SPEC-0003). */
sealed interface AnalysisUiEvent {
    /** Abrir el selector de imagen. */
    data object SelectImage : AnalysisUiEvent

    /** Quitar la imagen seleccionada y volver al estado vacío (RF-6). */
    data object RemoveImage : AnalysisUiEvent

    /** Cerrar el mensaje de error. */
    data object DismissError : AnalysisUiEvent

    /** Abre el diálogo que pide el código de paciente antes de escanear (SPEC-0005 RF-1). */
    data object StartScan : AnalysisUiEvent

    /** Texto del código de paciente en el diálogo (se sanea a dígitos y guion). */
    data class PatientCodeChanged(
        val code: String,
    ) : AnalysisUiEvent

    /**
     * Confirma el diálogo: guarda el análisis local (mock, andamiaje temporal) y, si hay cuenta, lo encola
     * y sincroniza con remoto (SPEC-0005 RF-3).
     */
    data object ConfirmScan : AnalysisUiEvent

    /** Cierra el diálogo de código sin escanear. */
    data object CancelScan : AnalysisUiEvent

    /** Reintenta la sincronización remota pendiente (RN-8). */
    data object RetrySync : AnalysisUiEvent

    /** Cierra el popup de error de sincronización. */
    data object DismissSyncError : AnalysisUiEvent

    data object DismissSavedConfirmation : AnalysisUiEvent
}
