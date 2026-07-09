package dev.lovelace.citovision.infrastructure.repositories

import dev.lovelace.citovision.application.ports.AnalysisImageStore
import dev.lovelace.citovision.application.ports.AnalysisRepository
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.Analysis
import dev.lovelace.citovision.domain.errors.AnalysisError
import dev.lovelace.citovision.infrastructure.persistence.database.AnalysisDao
import dev.lovelace.citovision.infrastructure.persistence.mappers.toCellCountEntities
import dev.lovelace.citovision.infrastructure.persistence.mappers.toDomain
import dev.lovelace.citovision.infrastructure.persistence.mappers.toEntity
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Implementación del puerto [AnalysisRepository] sobre Room (SPEC-0004). Traduce las excepciones de Room
 * y del sistema de ficheros a [AnalysisError]; ninguna `@Entity` sale de aquí.
 */
class AnalysisRepositoryImpl(
    private val dao: AnalysisDao,
    private val imageStore: AnalysisImageStore,
) : AnalysisRepository {
    override fun observeAnalyses(): Flow<List<Analysis>> =
        dao
            .observeAll()
            .map { rows -> rows.map { it.toDomain() } }
            .catch { error ->
                Napier.e("Fallo al observar los análisis", error)
                emit(emptyList())
            }

    override suspend fun saveAnalysis(analysis: Analysis): Result<Unit, AnalysisError> =
        try {
            dao.insertAnalysisWithCellCounts(analysis.toEntity(), analysis.toCellCountEntities())
            Result.Success(Unit)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Napier.e("Fallo al guardar el análisis", error)
            Result.Failure(AnalysisError.StorageFailure)
        }

    override suspend fun deleteAnalysis(id: String): Result<Unit, AnalysisError> =
        try {
            val imagePath = dao.findImagePath(id)
            val deletedRows = dao.deleteById(id)
            if (deletedRows == 0) {
                Result.Failure(AnalysisError.NotFound)
            } else {
                // El fichero es secundario: si no se puede borrar, se registra pero el borrado se confirma.
                imagePath?.let { path ->
                    if (imageStore.delete(path) is Result.Failure) {
                        Napier.w("El análisis se borró pero su imagen no pudo eliminarse")
                    }
                }
                Result.Success(Unit)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Napier.e("Fallo al borrar el análisis", error)
            Result.Failure(AnalysisError.StorageFailure)
        }
}
