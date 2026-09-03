package dev.lovelace.citovision.infrastructure.remote.storage

import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.errors.RemoteAnalysisError
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.serialization.ContentConvertException
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlin.coroutines.cancellation.CancellationException

/**
 * DataSource remoto de imágenes contra la API REST de Firebase Storage (SPEC-0005, ADR-0001).
 * Sube los bytes a `analyses/{ownerUid}/{analysisId}.<ext>` y devuelve la **URL de descarga** (con el
 * token que retorna la subida), que se guarda como `imageUrl` en el documento Firestore.
 *
 * El [client] inyectado es el **autorizado**: adjunta el ID token del usuario, que es lo que evalúan las
 * reglas de Storage sobre el `{ownerUid}` del path. Ojo: la URL de descarga que se devuelve lleva el
 * *download token* del objeto y da acceso a la imagen sin pasar por las reglas, por lo que solo debe
 * viajar dentro del documento Firestore del análisis (ya protegido). El [bucket] (público,
 * `*.firebasestorage.app`) se inyecta; los DTO no salen de aquí.
 */
class FirebaseStorageDataSource(
    private val client: HttpClient,
    private val bucket: String,
) {
    suspend fun uploadImage(
        ownerUid: String,
        analysisId: String,
        bytes: ByteArray,
        mimeType: String,
    ): Result<String, RemoteAnalysisError> =
        runCatchingStorage {
            val objectPath = "$ANALYSES_FOLDER/$ownerUid/$analysisId.${extensionFor(mimeType)}"
            val response =
                client
                    .post("$STORAGE_BASE/b/$bucket/o") {
                        parameter("uploadType", "media")
                        parameter("name", objectPath)
                        contentType(ContentType.parse(mimeType))
                        setBody(bytes)
                    }.body<StorageUploadResponse>()
            val token = response.downloadTokens
            if (token.isNullOrBlank()) {
                Result.Failure(RemoteAnalysisError.Unknown("missing download token"))
            } else {
                Result.Success(downloadUrl(objectPath, token))
            }
        }

    private fun downloadUrl(
        objectPath: String,
        token: String,
    ): String = "$STORAGE_BASE/b/$bucket/o/${objectPath.encodeURLParameter()}?alt=media&token=$token"

    private fun extensionFor(mimeType: String): String =
        when (mimeType) {
            "image/png" -> "png"
            else -> "jpg"
        }

    private suspend fun <T> runCatchingStorage(
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
                Napier.w("Storage respondió ${status.value}", e, tag = "Storage")
                Result.Failure(RemoteAnalysisError.Unknown("HTTP ${status.value}"))
            }
        } catch (e: ServerResponseException) {
            Result.Failure(RemoteAnalysisError.Unknown("HTTP ${e.response.status.value}"))
        } catch (e: HttpRequestTimeoutException) {
            Result.Failure(RemoteAnalysisError.Network)
        } catch (e: ContentConvertException) {
            Napier.w("Respuesta de Storage ilegible", e, tag = "Storage")
            Result.Failure(RemoteAnalysisError.Serialization)
        } catch (e: IOException) {
            // Fallo real de entrada/salida: sin conexión, DNS que no resuelve, socket cortado, TLS.
            Napier.w("Fallo de red en Storage", e, tag = "Storage")
            Result.Failure(RemoteAnalysisError.Network)
        } catch (e: Exception) {
            // Lo desconocido se admite como desconocido: anunciarlo como falta de conexión manda a buscar
            // donde no está. El tipo va al log, nunca a la UI.
            Napier.w("Fallo inesperado en Storage", e, tag = "Storage")
            Result.Failure(RemoteAnalysisError.Unknown(e::class.simpleName))
        }

    private companion object {
        const val STORAGE_BASE = "https://firebasestorage.googleapis.com/v0"
        const val ANALYSES_FOLDER = "analyses"
    }
}

@Serializable
data class StorageUploadResponse(
    val name: String? = null,
    val bucket: String? = null,
    val downloadTokens: String? = null,
)
