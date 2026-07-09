package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.ImagePicker
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.core.result.fold
import dev.lovelace.citovision.domain.entities.SelectedImage
import dev.lovelace.citovision.domain.errors.ImageError

/**
 * Abre el selector de imagen y valida el resultado contra las reglas de negocio (SPEC-0003):
 * formato permitido (RN-1) y tamaño máximo (RN-2). Una cancelación se propaga como `Success(null)`.
 */
class PickImageUseCase(
    private val imagePicker: ImagePicker,
) {
    suspend operator fun invoke(): Result<SelectedImage?, ImageError> =
        imagePicker.pickImage().fold(
            onSuccess = { image ->
                when {
                    image == null -> Result.Success(null) // cancelado
                    image.mimeType !in ALLOWED_MIME_TYPES -> Result.Failure(ImageError.UnsupportedFormat)
                    image.sizeBytes > MAX_SIZE_BYTES -> Result.Failure(ImageError.TooLarge)
                    else -> Result.Success(image)
                }
            },
            onFailure = { error -> Result.Failure(error) },
        )

    companion object {
        /** RN-1: solo JPG y PNG (SPEC-0003). */
        val ALLOWED_MIME_TYPES = setOf("image/jpeg", "image/png")

        /** RN-2: 10 MB (SPEC-0003). */
        const val MAX_SIZE_BYTES = 10L * 1024 * 1024
    }
}
