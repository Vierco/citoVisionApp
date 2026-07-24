package dev.lovelace.citovision.infrastructure.remote.firestore

import dev.lovelace.citovision.application.ports.RemoteFeedback
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.Feedback
import dev.lovelace.citovision.domain.errors.RemoteFeedbackError
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.ContentConvertException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlin.time.Clock

/**
 * DataSource remoto de feedback contra la API REST de Cloud Firestore. Escribe cada feedback como un
 * documento nuevo en la colección `feedback` mediante `PATCH` con id generado en cliente (upsert).
 *
 * El [client] inyectado es el **autorizado**: las reglas solo admiten `create` de usuarios con cuenta,
 * así que el envío exige sesión (lo filtra antes `SubmitFeedbackUseCase`). La `apiKey` (Web API key,
 * pública) identifica al proyecto, no al usuario. Los DTO no salen de aquí.
 */
class FirestoreFeedbackDataSource(
    private val client: HttpClient,
    private val projectId: String,
    private val apiKey: String,
) : RemoteFeedback {
    override suspend fun submit(feedback: Feedback): Result<Unit, RemoteFeedbackError> =
        runCatchingFirestore {
            client.patch(documentUrl(generateId())) {
                parameter("key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(FirestoreDocument(fields = feedbackToFields(feedback)))
            }
            Result.Success(Unit)
        }

    private fun feedbackToFields(feedback: Feedback): Map<String, FirestoreValue> =
        buildMap {
            put("email", FirestoreValue(stringValue = feedback.email))
            put("message", FirestoreValue(stringValue = feedback.message))
            put("createdAt", FirestoreValue(integerValue = feedback.createdAt.toEpochMilliseconds().toString()))
            feedback.ownerUid?.let { put("ownerUid", FirestoreValue(stringValue = it)) }
        }

    private fun documentUrl(id: String): String =
        "$DOCUMENTS_BASE/projects/$projectId/databases/(default)/documents/$COLLECTION/$id"

    private fun generateId(): String =
        "${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt(MIN_SUFFIX, MAX_SUFFIX)}"

    private suspend fun runCatchingFirestore(
        block: suspend () -> Result<Unit, RemoteFeedbackError>,
    ): Result<Unit, RemoteFeedbackError> =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: ClientRequestException) {
            Napier.w("Firestore respondió ${e.response.status.value}", e, tag = "Firestore")
            Result.Failure(RemoteFeedbackError.Unknown("HTTP ${e.response.status.value}"))
        } catch (e: ServerResponseException) {
            Result.Failure(RemoteFeedbackError.Unknown("HTTP ${e.response.status.value}"))
        } catch (e: HttpRequestTimeoutException) {
            Result.Failure(RemoteFeedbackError.Network)
        } catch (e: ContentConvertException) {
            Napier.w("Respuesta de Firestore ilegible", e, tag = "Firestore")
            Result.Failure(RemoteFeedbackError.Serialization)
        } catch (e: Exception) {
            Napier.w("Fallo no tipado en Firestore", e, tag = "Firestore")
            Result.Failure(RemoteFeedbackError.Network)
        }

    private companion object {
        const val DOCUMENTS_BASE = "https://firestore.googleapis.com/v1"
        const val COLLECTION = "feedback"
        const val MIN_SUFFIX = 1000
        const val MAX_SUFFIX = 9999
    }
}
