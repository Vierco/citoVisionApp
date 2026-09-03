package dev.lovelace.citovision.presentation.events

/** Acciones de usuario de la pestaña Análisis, resueltas en AnalysisViewModel.onEvent() (SPEC-0003). */
sealed interface AnalysisUiEvent {
    /** Abrir el selector de imagen. */
    data object SelectImage : AnalysisUiEvent

    /** Quitar la imagen seleccionada y volver al estado vacío (RF-6). */
    data object RemoveImage : AnalysisUiEvent

    /** Cierra el aviso único del origen de las imágenes y continúa abriendo el selector. */
    data object DismissImageSourceNotice : AnalysisUiEvent

    /** Cerrar el mensaje de error. */
    data object DismissError : AnalysisUiEvent

    /** Abre el diálogo que pide el código de paciente antes de escanear (SPEC-0005 RF-1). */
    data object StartScan : AnalysisUiEvent

    /** Texto del código de paciente en el diálogo (se sanea a dígitos y guion). */
    data class PatientCodeChanged(
        val code: String,
    ) : AnalysisUiEvent

    /**
     * Confirma el diálogo: ejecuta el modelo sobre la imagen (SPEC-0006), y si hay células persiste el
     * análisis local y, con cuenta, lo encola y sincroniza con remoto (SPEC-0005 RF-3).
     */
    data object ConfirmScan : AnalysisUiEvent

    /** Cierra el diálogo de código sin escanear. */
    data object CancelScan : AnalysisUiEvent

    /** Reintenta el análisis con la misma imagen tras un fallo de inferencia (SPEC-0006 RF-7). */
    data object RetryAnalysis : AnalysisUiEvent

    /** Cierra el popup informativo de "sin células detectadas" (SPEC-0006 RF-6). */
    data object DismissNoCells : AnalysisUiEvent

    /** Cierra el popup de error de inferencia. */
    data object DismissInferenceError : AnalysisUiEvent

    /** Reintenta la sincronización remota pendiente (RN-8). */
    data object RetrySync : AnalysisUiEvent

    /** Cierra el popup de error de sincronización. */
    data object DismissSyncError : AnalysisUiEvent

    data object DismissSavedConfirmation : AnalysisUiEvent
}
