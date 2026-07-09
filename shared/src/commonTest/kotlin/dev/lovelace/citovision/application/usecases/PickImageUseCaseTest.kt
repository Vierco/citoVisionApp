package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.ImagePicker
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.SelectedImage
import dev.lovelace.citovision.domain.errors.ImageError
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * El use case aplica las reglas de negocio de SPEC-0003 sobre lo que devuelve el puerto [ImagePicker]:
 * formato permitido (RN-1) y tamaño máximo (RN-2). La cancelación se propaga como `Success(null)`.
 */
class PickImageUseCaseTest {
    private val imagePicker = mock<ImagePicker>()
    private val useCase = PickImageUseCase(imagePicker)

    private fun image(
        mimeType: String,
        sizeBytes: Long,
    ) = SelectedImage(bytes = ByteArray(1), fileName = "muestra.jpg", mimeType = mimeType, sizeBytes = sizeBytes)

    @Test
    fun `given a valid JPG when picking then returns the image`() =
        runTest {
            // Given
            val picked = image("image/jpeg", sizeBytes = 1_000)
            everySuspend { imagePicker.pickImage() } returns Result.Success(picked)

            // When
            val result = useCase()

            // Then
            assertEquals(Result.Success(picked), result)
        }

    @Test
    fun `given the user cancels when picking then returns success with null`() =
        runTest {
            // Given
            everySuspend { imagePicker.pickImage() } returns Result.Success(null)

            // When
            val result = useCase()

            // Then
            assertEquals(Result.Success(null), result)
        }

    @Test
    fun `given an unsupported format when picking then fails with UnsupportedFormat`() =
        runTest {
            // Given
            everySuspend { imagePicker.pickImage() } returns Result.Success(image("image/gif", sizeBytes = 1_000))

            // When
            val result = useCase()

            // Then
            assertEquals(Result.Failure(ImageError.UnsupportedFormat), result)
        }

    @Test
    fun `given an image over the size limit when picking then fails with TooLarge`() =
        runTest {
            // Given
            val tooBig = PickImageUseCase.MAX_SIZE_BYTES + 1
            everySuspend { imagePicker.pickImage() } returns Result.Success(image("image/png", sizeBytes = tooBig))

            // When
            val result = useCase()

            // Then
            assertEquals(Result.Failure(ImageError.TooLarge), result)
        }

    @Test
    fun `given the picker fails when picking then propagates the failure`() =
        runTest {
            // Given
            everySuspend { imagePicker.pickImage() } returns Result.Failure(ImageError.ReadFailed)

            // When
            val result = useCase()

            // Then
            assertEquals(Result.Failure(ImageError.ReadFailed), result)
        }
}
