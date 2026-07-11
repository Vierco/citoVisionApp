package dev.lovelace.citovision.infrastructure.remote

import dev.lovelace.citovision.application.ports.AnalysisImageStore
import dev.lovelace.citovision.application.ports.RemoteAnalysisSync
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.Analysis
import dev.lovelace.citovision.domain.errors.RemoteAnalysisError
import dev.lovelace.citovision.infrastructure.persistence.database.AnalysisDao
import dev.lovelace.citovision.infrastructure.persistence.database.RemoteUploadOutboxDao
import dev.lovelace.citovision.infrastructure.persistence.database.RemoteUploadOutboxEntity
import dev.lovelace.citovision.infrastructure.persistence.mappers.toDomain
import dev.lovelace.citovision.infrastructure.remote.firestore.FirestoreAnalysisDataSource
import dev.lovelace.citovision.infrastructure.remote.storage.FirebaseStorageDataSource
import io.github.aakira.napier.Napier
import kotlin.time.Clock

/**
 * Procesador del outbox (SPEC-0005, RN-8). Encola análisis para sincronizar y drena la cola: por cada
 * entrada lee el análisis local, sube su imagen a Storage (si la hay), escribe el documento en Firestore
 * y borra la entrada. Idempotente (mismo UUID → upsert de documento y path de Storage estables). Un
 * reintento automático por entrada; si persiste, la deja en `FAILED` para reintento manual. No loguea
 * datos sensibles.
 */
class RemoteAnalysisSyncImpl(
    private val outboxDao: RemoteUploadOutboxDao,
    private val analysisDao: AnalysisDao,
    private val imageStore: AnalysisImageStore,
    private val firestore: FirestoreAnalysisDataSource,
    private val storage: FirebaseStorageDataSource,
    private val clock: Clock = Clock.System,
) : RemoteAnalysisSync {
    override suspend fun enqueue(
        analysisId: String,
        ownerUid: String,
    ) {
        outboxDao.upsert(
            RemoteUploadOutboxEntity(
                analysisId = analysisId,
                ownerUid = ownerUid,
                status = STATUS_PENDING,
                attempts = 0,
                lastError = null,
                createdAt = clock.now().toEpochMilliseconds(),
            ),
        )
    }

    override suspend fun processPending(): Result<Unit, RemoteAnalysisError> {
        var lastError: RemoteAnalysisError? = null
        for (entry in outboxDao.getAll()) {
            when (val result = pushWithRetry(entry)) {
                is Result.Success -> outboxDao.deleteById(entry.analysisId)
                is Result.Failure -> {
                    lastError = result.error
                    outboxDao.upsert(
                        entry.copy(
                            status = STATUS_FAILED,
                            attempts = entry.attempts + 1,
                            lastError = result.error.toString(),
                        ),
                    )
                }
            }
        }
        return lastError?.let { Result.Failure(it) } ?: Result.Success(Unit)
    }

    /** Un reintento automático (RN-8): si el primer empuje falla, se intenta una vez más. */
    private suspend fun pushWithRetry(entry: RemoteUploadOutboxEntity): Result<Unit, RemoteAnalysisError> {
        val first = push(entry)
        return if (first is Result.Failure) push(entry) else first
    }

    private suspend fun push(entry: RemoteUploadOutboxEntity): Result<Unit, RemoteAnalysisError> {
        val analysis =
            analysisDao.findById(entry.analysisId)?.toDomain()
                ?: return Result.Success(Unit) // el análisis local ya no existe: nada que sincronizar
        return when (val imageResult = uploadImageIfPresent(entry.ownerUid, analysis)) {
            is Result.Success -> firestore.saveAnalysis(entry.ownerUid, analysis, imageResult.value)
            is Result.Failure -> imageResult
        }
    }

    private suspend fun uploadImageIfPresent(
        ownerUid: String,
        analysis: Analysis,
    ): Result<String?, RemoteAnalysisError> {
        val path = analysis.imagePath ?: return Result.Success(null)
        return when (val bytes = imageStore.read(path)) {
            is Result.Success -> storage.uploadImage(ownerUid, analysis.id, bytes.value, mimeTypeFor(path))
            is Result.Failure -> {
                Napier.w("No se pudo leer la imagen local para sincronizar", tag = "OutboxSync")
                Result.Failure(RemoteAnalysisError.Unknown("local image read failed"))
            }
        }
    }

    private fun mimeTypeFor(path: String): String =
        if (path.substringAfterLast('.').lowercase() == "png") "image/png" else "image/jpeg"

    private companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_FAILED = "FAILED"
    }
}
