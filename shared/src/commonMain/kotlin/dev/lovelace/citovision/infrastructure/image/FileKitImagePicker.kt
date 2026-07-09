package dev.lovelace.citovision.infrastructure.image

import dev.lovelace.citovision.application.ports.ImagePicker
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.SelectedImage
import dev.lovelace.citovision.domain.errors.ImageError
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.CancellationException

/**
 * Implementación de [ImagePicker] con FileKit (SPEC-0003). Vive en commonMain porque FileKit expone una
 * API común multiplataforma; en Android requiere `FileKit.init(activity)` (ver MainActivity). El MIME se
 * deriva de la extensión del nombre; la validación de formato/tamaño la hace [PickImageUseCase].
 */
class FileKitImagePicker : ImagePicker {
    override suspend fun pickImage(): Result<SelectedImage?, ImageError> {
        return try {
            val file =
                FileKit.openFilePicker(type = FileKitType.Image)
                    ?: return Result.Success(null) // cancelado por el usuario
            val bytes = file.readBytes()
            val image =
                SelectedImage(
                    bytes = bytes,
                    fileName = file.name,
                    mimeType = mimeTypeFor(file.name),
                    sizeBytes = bytes.size.toLong(),
                )
            Result.Success(image)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            // Nunca se loguea el contenido de la imagen (dato médico sensible, SPEC-0003 RNF-5).
            Napier.e("Fallo al leer la imagen seleccionada", error)
            Result.Failure(ImageError.ReadFailed)
        }
    }

    private fun mimeTypeFor(fileName: String): String =
        when (fileName.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "application/octet-stream"
        }
}
