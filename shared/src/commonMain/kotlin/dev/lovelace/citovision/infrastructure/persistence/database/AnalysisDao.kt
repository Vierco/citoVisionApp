package dev.lovelace.citovision.infrastructure.persistence.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a los análisis. Las consultas observables devuelven `Flow`; las acciones puntuales son `suspend`
 * (requisito de Room KMP para targets no-Android).
 */
@Dao
interface AnalysisDao {
    @Transaction
    @Query("SELECT * FROM analyses ORDER BY performedAt DESC")
    fun observeAll(): Flow<List<AnalysisWithCellCounts>>

    @Insert
    suspend fun insertAnalysis(analysis: AnalysisEntity)

    @Insert
    suspend fun insertCellCounts(entries: List<CellCountEntity>)

    /** Inserta análisis y conteo en la misma transacción: o entran ambos, o ninguno. */
    @Transaction
    suspend fun insertAnalysisWithCellCounts(
        analysis: AnalysisEntity,
        entries: List<CellCountEntity>,
    ) {
        insertAnalysis(analysis)
        insertCellCounts(entries)
    }

    /** Ruta de la imagen, necesaria para borrar el fichero tras eliminar la fila (RN-7). */
    @Query("SELECT imagePath FROM analyses WHERE id = :id")
    suspend fun findImagePath(id: String): String?

    /** Devuelve el número de filas borradas: 0 significa que el análisis no existía. */
    @Query("DELETE FROM analyses WHERE id = :id")
    suspend fun deleteById(id: String): Int
}
