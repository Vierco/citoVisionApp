package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.RemotePatientAnalyses
import dev.lovelace.citovision.core.result.Result

/**
 * Borra un análisis de la base de datos remota desde la pestaña Pacientes (SPEC-0005). Es **independiente**
 * del historial local (RF-9): no toca la BD local. Devuelve `true` si el borrado remoto se completó.
 */
class DeleteRemoteAnalysisUseCase(
    private val remotePatientAnalyses: RemotePatientAnalyses,
) {
    suspend operator fun invoke(analysisId: String): Boolean =
        when (remotePatientAnalyses.deleteAnalysis(analysisId)) {
            is Result.Success -> true
            is Result.Failure -> false
        }
}
