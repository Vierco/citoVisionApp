package dev.lovelace.citovision.infrastructure.image

import dev.lovelace.citovision.application.ports.AnalysisImageStore
import dev.lovelace.citovision.core.coroutines.ioDispatcher
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.errors.AnalysisError
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * Guarda las imágenes de análisis como ficheros dentro del almacenamiento privado de la app
 * (SPEC-0004 RN-4). El nombre de fichero lo aporta quien llama y debe ser único.
 * Nunca se loguea el contenido de la imagen (dato médico).
 */
class OkioAnalysisImageStore(
    private val baseDirectory: String,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val dispatcher: CoroutineDispatcher = ioDispatcher,
) : AnalysisImageStore {
    override suspend fun save(
        bytes: ByteArray,
        fileName: String,
    ): Result<String, AnalysisError> =
        withContext(dispatcher) {
            try {
                val directory = baseDirectory.toPath()
                fileSystem.createDirectories(directory)
                val target = directory / fileName
                fileSystem.write(target) { write(bytes) }
                Result.Success(target.toString())
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Napier.e("Fallo al guardar la imagen del análisis", error)
                Result.Failure(AnalysisError.StorageFailure)
            }
        }

    override suspend fun delete(path: String): Result<Unit, AnalysisError> =
        withContext(dispatcher) {
            try {
                // mustExist = false: que el fichero ya no esté no es un error.
                fileSystem.delete(path.toPath(), mustExist = false)
                Result.Success(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Napier.e("Fallo al borrar la imagen del análisis", error)
                Result.Failure(AnalysisError.StorageFailure)
            }
        }
}
