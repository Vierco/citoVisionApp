package dev.lovelace.citovision.infrastructure.persistence.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cola local (transactional outbox, SPEC-0005) de análisis pendientes de sincronizar con remoto.
 * La clave es el `analysisId` (una entrada por análisis): permite reencolar de forma idempotente y
 * correlaciona con el análisis local, que es la fuente del payload. No sale de Infrastructure.
 *
 * [status] = PENDING | FAILED (FAILED = agotados los reintentos automáticos, a la espera de reintento
 * manual). [attempts] cuenta los intentos de empuje; [lastError] guarda el último fallo (sin datos
 * sensibles). El orden de proceso es por [createdAt] ascendente.
 */
@Entity(tableName = "remote_upload_outbox")
data class RemoteUploadOutboxEntity(
    @PrimaryKey val analysisId: String,
    val ownerUid: String,
    val status: String,
    val attempts: Int,
    val lastError: String?,
    val createdAt: Long,
)
