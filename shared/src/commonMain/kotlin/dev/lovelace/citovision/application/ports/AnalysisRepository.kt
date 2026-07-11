package dev.lovelace.citovision.application.ports

import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.Analysis
import dev.lovelace.citovision.domain.errors.AnalysisError
import kotlinx.coroutines.flow.Flow

/**
 * Puerto de persistencia de análisis (SPEC-0004). La implementación vive en `infrastructure/repositories`
 * sobre Room; ninguna `@Entity` de Room cruza este contrato.
 */
interface AnalysisRepository {
    /** Análisis persistidos, ordenados por fecha descendente. Re-emite ante cualquier cambio (RF-1). */
    fun observeAnalyses(): Flow<List<Analysis>>

    /** Persiste un análisis nuevo junto a su conteo celular. */
    suspend fun saveAnalysis(analysis: Analysis): Result<Unit, AnalysisError>

    /** Borra el análisis, sus entradas de conteo (cascada) y su fichero de imagen (RN-7). */
    suspend fun deleteAnalysis(id: String): Result<Unit, AnalysisError>

    /** Borra todos los análisis locales, su conteo (cascada) y sus ficheros de imagen. */
    suspend fun deleteAllAnalyses(): Result<Unit, AnalysisError>
}
