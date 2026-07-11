package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.AnalysisRepository
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.Analysis
import dev.lovelace.citovision.domain.errors.AnalysisError
import kotlinx.coroutines.flow.Flow

/** Análisis persistidos, ordenados por fecha descendente (SPEC-0004 RF-1). */
class ObserveAnalysesUseCase(
    private val analysisRepository: AnalysisRepository,
) {
    operator fun invoke(): Flow<List<Analysis>> = analysisRepository.observeAnalyses()
}

/**
 * Borra un análisis (SPEC-0004 RF-5). El borrado es **idempotente**: que el análisis ya no exista
 * (`NotFound`, p. ej. por una doble pulsación) se considera éxito, porque el estado final es el deseado.
 */
class DeleteAnalysisUseCase(
    private val analysisRepository: AnalysisRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit, AnalysisError> =
        when (val result = analysisRepository.deleteAnalysis(id)) {
            is Result.Success -> result
            is Result.Failure ->
                if (result.error == AnalysisError.NotFound) Result.Success(Unit) else result
        }
}

/** Borra todos los análisis locales (acción de Ajustes). No afecta a la base de datos remota (RF-9). */
class DeleteAllAnalysesUseCase(
    private val analysisRepository: AnalysisRepository,
) {
    suspend operator fun invoke(): Result<Unit, AnalysisError> = analysisRepository.deleteAllAnalyses()
}
