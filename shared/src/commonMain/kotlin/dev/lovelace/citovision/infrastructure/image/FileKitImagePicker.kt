package dev.lovelace.citovision.infrastructure.image

import dev.lovelace.citovision.application.ports.ImagePicker
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.SelectedImage
import dev.lovelace.citovision.domain.errors.ImageError
import dev.lovelace.citovision.domain.settings.ImageSourcePreference
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
 *
 * El tipo que se le pide a FileKit **decide qué selector nativo se abre**, y son excluyentes:
 *
 * - [FileKitType.Image] → `PickVisualMedia` en Android y `PHPickerViewController` en iOS: la fototeca,
 *   y solo la fototeca. Un fichero suelto en una carpeta es invisible para ellos.
 * - [FileKitType.File] → `ACTION_OPEN_DOCUMENT` (SAF) en Android y `UIDocumentPickerViewController` en
 *   iOS: carpetas, descargas, unidades y proveedores externos, pero **no** la fototeca.
 *
 * Ninguno de los dos exige permisos de almacenamiento, que es lo que pedía SPEC-0003 §Seguridad.
 *
 * En Desktop ambas ramas acaban en el mismo diálogo nativo y solo cambia el filtro de extensiones.
 */
class FileKitImagePicker : ImagePicker {
    override val hasDistinctSources: Boolean = platformHasDistinctImageSources

    override val canOpenPickerAfterDialog: Boolean = platformCanOpenPickerAfterDialog

    override suspend fun pickImage(source: ImageSourcePreference): Result<SelectedImage?, ImageError> {
        return try {
            val file =
                FileKit.openFilePicker(type = source.toFileKitType())
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

    /**
     * El explorador de ficheros se restringe a las extensiones que la app admite (RN-1), en vez de
     * abrirlo a cualquier fichero: así el usuario no puede elegir algo que después se rechazaría.
     */
    private fun ImageSourcePreference.toFileKitType(): FileKitType =
        when (this) {
            ImageSourcePreference.GALLERY -> FileKitType.Image
            ImageSourcePreference.FILES -> FileKitType.File(ALLOWED_EXTENSIONS)
        }

    private fun mimeTypeFor(fileName: String): String =
        when (fileName.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "application/octet-stream"
        }

    private companion object {
        val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png")
    }
}

/**
 * Si la plataforma distingue entre fototeca y ficheros. Es una diferencia real de plataforma, no una
 * decisión de producto, así que se resuelve con `expect/actual` (AGENTS.md §3).
 */
internal expect val platformHasDistinctImageSources: Boolean

/**
 * Si el selector puede abrirse justo después de cerrar un diálogo. Solo iOS lo impide, por cómo Compose
 * monta allí los diálogos y cómo FileKit elige el controlador sobre el que presentar.
 */
internal expect val platformCanOpenPickerAfterDialog: Boolean
