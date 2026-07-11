package dev.lovelace.citovision.application.ports

import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.Feedback
import dev.lovelace.citovision.domain.errors.RemoteFeedbackError

/** Puerto de envío de feedback a la base de datos remota. La implementación (Firestore REST) vive en Infrastructure. */
interface RemoteFeedback {
    suspend fun submit(feedback: Feedback): Result<Unit, RemoteFeedbackError>
}
