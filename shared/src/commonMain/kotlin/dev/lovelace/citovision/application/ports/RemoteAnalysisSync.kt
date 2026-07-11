package dev.lovelace.citovision.application.ports

import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.errors.RemoteAnalysisError

/**
 * Sincronización remota de análisis (SPEC-0005, patrón outbox). [enqueue] registra un análisis local para
 * empujarlo a remoto (idempotente por analysisId); [processPending] drena la cola. La implementación vive
 * en `infrastructure` y orquesta la cola local, el análisis local, Firebase Storage y Firestore.
 */
interface RemoteAnalysisSync {
    suspend fun enqueue(
        analysisId: String,
        ownerUid: String,
    )

    /** Drena la cola. Devuelve `Failure` con el último error si alguna entrada no pudo empujarse (RN-8). */
    suspend fun processPending(): Result<Unit, RemoteAnalysisError>
}
