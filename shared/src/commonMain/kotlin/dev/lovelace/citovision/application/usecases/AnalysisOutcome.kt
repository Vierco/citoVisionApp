package dev.lovelace.citovision.application.usecases

/**
 * Resultado de [AnalyzeSampleUseCase] (SPEC-0006). Distingue el éxito —que lleva el id para sincronizar— de
 * los casos que **no persisten**: sin células reales detectadas (RN-7), fallo de inferencia (RF-7) o fallo
 * al guardar en local.
 */
sealed interface AnalysisOutcome {
    data class Saved(
        val analysisId: String,
    ) : AnalysisOutcome

    data object NoCellsDetected : AnalysisOutcome

    data object InferenceFailed : AnalysisOutcome

    data object SaveFailed : AnalysisOutcome
}
