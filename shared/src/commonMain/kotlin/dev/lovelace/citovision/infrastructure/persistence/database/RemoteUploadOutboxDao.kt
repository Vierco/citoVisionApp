package dev.lovelace.citovision.infrastructure.persistence.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/**
 * Acceso a la cola de sincronización remota (SPEC-0005). Acciones puntuales `suspend` (Room KMP).
 * El `@Upsert` sobre la clave `analysisId` hace idempotente el (re)encolado y la actualización de estado.
 */
@Dao
interface RemoteUploadOutboxDao {
    @Upsert
    suspend fun upsert(entry: RemoteUploadOutboxEntity)

    /** Entradas pendientes de empujar, en orden de creación (las más antiguas primero). */
    @Query("SELECT * FROM remote_upload_outbox ORDER BY createdAt ASC")
    suspend fun getAll(): List<RemoteUploadOutboxEntity>

    @Query("SELECT * FROM remote_upload_outbox WHERE analysisId = :analysisId")
    suspend fun findById(analysisId: String): RemoteUploadOutboxEntity?

    @Query("DELETE FROM remote_upload_outbox WHERE analysisId = :analysisId")
    suspend fun deleteById(analysisId: String)

    @Query("SELECT COUNT(*) FROM remote_upload_outbox")
    suspend fun count(): Int
}
