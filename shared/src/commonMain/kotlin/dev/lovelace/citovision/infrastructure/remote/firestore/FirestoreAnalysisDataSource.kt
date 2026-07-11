package dev.lovelace.citovision.infrastructure.remote.firestore

import dev.lovelace.citovision.application.ports.RemotePatientAnalyses
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.Analysis
import dev.lovelace.citovision.domain.errors.RemoteAnalysisError
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.delete
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.ContentConvertException
import kotlin.coroutines.cancellation.CancellationException

/**
 * DataSource remoto de análisis contra la API REST de Cloud Firestore (SPEC-0005, ADR-0001).
 *
 * - Escritura: `PATCH` sobre `analyses/{id}` → upsert idempotente por UUID (RN-6).
 * - Consulta: `:runQuery` filtrando `ownerUid` == y `patientCode` == (ambos *equality*, sin índice
 *   compuesto); el orden descendente por fecha (RN-4) se aplica en cliente.
 *
 * Con reglas públicas (dev) no hace falta token; la `apiKey` (Web API key, pública) identifica el
 * proyecto. Al cerrar reglas se añadirá el ID token vía el interceptor de auth. Los DTO no salen de aquí.
 */
class FirestoreAnalysisDataSource(
    private val client: HttpClient,
    private val projectId: String,
    private val apiKey: String,
) : RemotePatientAnalyses {
    suspend fun saveAnalysis(
        ownerUid: String,
        analysis: Analysis,
        imageUrl: String?,
    ): Result<Unit, RemoteAnalysisError> =
        runCatchingFirestore {
            client.patch(documentUrl(analysis.id)) {
                parameter("key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(FirestoreDocument(fields = analysisToFirestoreFields(ownerUid, analysis, imageUrl)))
            }
            Result.Success(Unit)
        }

    override suspend fun queryByPatient(
        ownerUid: String,
        patientCode: String,
    ): Result<List<Analysis>, RemoteAnalysisError> =
        runCatchingFirestore {
            val items =
                client
                    .post(runQueryUrl()) {
                        parameter("key", apiKey)
                        contentType(ContentType.Application.Json)
                        setBody(buildPatientQuery(ownerUid, patientCode))
                    }.body<List<RunQueryResponseItem>>()
            val analyses =
                items
                    .mapNotNull { it.document }
                    .map { it.toAnalysis() }
                    .sortedByDescending { it.performedAt }
            Result.Success(analyses)
        }

    override suspend fun deleteAnalysis(analysisId: String): Result<Unit, RemoteAnalysisError> =
        runCatchingFirestore {
            client.delete(documentUrl(analysisId)) {
                parameter("key", apiKey)
            }
            Result.Success(Unit)
        }

    private fun documentUrl(id: String): String =
        "$DOCUMENTS_BASE/projects/$projectId/databases/(default)/documents/$COLLECTION/$id"

    private fun runQueryUrl(): String = "$DOCUMENTS_BASE/projects/$projectId/databases/(default)/documents:runQuery"

    private fun buildPatientQuery(
        ownerUid: String,
        patientCode: String,
    ): RunQueryRequest =
        RunQueryRequest(
            structuredQuery =
                StructuredQuery(
                    from = listOf(CollectionSelector(collectionId = COLLECTION)),
                    where =
                        Filter(
                            compositeFilter =
                                CompositeFilter(
                                    op = "AND",
                                    filters =
                                        listOf(
                                            equalityFilter("ownerUid", ownerUid),
                                            equalityFilter("patientCode", patientCode),
                                        ),
                                ),
                        ),
                ),
        )

    private fun equalityFilter(
        fieldPath: String,
        value: String,
    ): FieldFilterWrapper =
        FieldFilterWrapper(
            fieldFilter =
                FieldFilter(
                    field = FieldReference(fieldPath = fieldPath),
                    op = "EQUAL",
                    value = FirestoreValue(stringValue = value),
                ),
        )

    private suspend fun <T> runCatchingFirestore(
        block: suspend () -> Result<T, RemoteAnalysisError>,
    ): Result<T, RemoteAnalysisError> =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: ClientRequestException) {
            val status = e.response.status
            if (status == HttpStatusCode.Unauthorized || status == HttpStatusCode.Forbidden) {
                Result.Failure(RemoteAnalysisError.Unauthorized)
            } else {
                Napier.w("Firestore respondió ${status.value}", e, tag = "Firestore")
                Result.Failure(RemoteAnalysisError.Unknown("HTTP ${status.value}"))
            }
        } catch (e: ServerResponseException) {
            Result.Failure(RemoteAnalysisError.Unknown("HTTP ${e.response.status.value}"))
        } catch (e: HttpRequestTimeoutException) {
            Result.Failure(RemoteAnalysisError.Network)
        } catch (e: ContentConvertException) {
            Napier.w("Respuesta de Firestore ilegible", e, tag = "Firestore")
            Result.Failure(RemoteAnalysisError.Serialization)
        } catch (e: Exception) {
            Napier.w("Fallo no tipado en Firestore", e, tag = "Firestore")
            Result.Failure(RemoteAnalysisError.Network)
        }

    private companion object {
        const val DOCUMENTS_BASE = "https://firestore.googleapis.com/v1"
        const val COLLECTION = "analyses"
    }
}
