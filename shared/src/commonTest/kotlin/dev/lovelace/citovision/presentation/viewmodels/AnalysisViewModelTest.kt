package dev.lovelace.citovision.presentation.viewmodels

import dev.lovelace.citovision.application.ports.ImagePicker
import dev.lovelace.citovision.application.usecases.PickImageUseCase
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.SelectedImage
import dev.lovelace.citovision.domain.errors.ImageError
import dev.lovelace.citovision.presentation.events.AnalysisUiEvent
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mokkery no puede mockear clases final (el use case), así que se construye el [PickImageUseCase] real
 * con el puerto [ImagePicker] mockeado.
 */
class AnalysisViewModelTest {
    private val imagePicker = mock<ImagePicker>()
    private val pickImage = PickImageUseCase(imagePicker)
    private val dispatcher = StandardTestDispatcher()

    private fun buildViewModel() = AnalysisViewModel(pickImage)

    private val validImage =
        SelectedImage(bytes = ByteArray(4), fileName = "muestra.png", mimeType = "image/png", sizeBytes = 4)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given a valid image when selecting then stores it and enables scan`() =
        runTest(dispatcher) {
            // Given
            everySuspend { imagePicker.pickImage() } returns Result.Success(validImage)
            val viewModel = buildViewModel()

            // When
            viewModel.onEvent(AnalysisUiEvent.SelectImage)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertEquals(validImage, state.selectedImage)
            assertTrue(state.canScan)
            assertFalse(state.isPicking)
            assertNull(state.error)
        }

    @Test
    fun `given the user cancels when selecting then keeps the previous state`() =
        runTest(dispatcher) {
            // Given
            everySuspend { imagePicker.pickImage() } returns Result.Success(null)
            val viewModel = buildViewModel()

            // When
            viewModel.onEvent(AnalysisUiEvent.SelectImage)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertNull(state.selectedImage)
            assertFalse(state.canScan)
            assertFalse(state.isPicking)
            assertNull(state.error)
        }

    @Test
    fun `given an unsupported format when selecting then shows an error and does not enable scan`() =
        runTest(dispatcher) {
            // Given
            everySuspend { imagePicker.pickImage() } returns
                Result.Success(SelectedImage(ByteArray(1), "muestra.gif", "image/gif", 1))
            val viewModel = buildViewModel()

            // When
            viewModel.onEvent(AnalysisUiEvent.SelectImage)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertNull(state.selectedImage)
            assertFalse(state.canScan)
            assertTrue(state.error != null)
        }

    @Test
    fun `given a selected image when removing then returns to the empty state`() =
        runTest(dispatcher) {
            // Given
            everySuspend { imagePicker.pickImage() } returns Result.Success(validImage)
            val viewModel = buildViewModel()
            viewModel.onEvent(AnalysisUiEvent.SelectImage)
            advanceUntilIdle()

            // When
            viewModel.onEvent(AnalysisUiEvent.RemoveImage)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.selectedImage)
            assertFalse(state.canScan)
        }

    @Test
    fun `given an error is shown when dismissing then clears the error`() =
        runTest(dispatcher) {
            // Given
            everySuspend { imagePicker.pickImage() } returns Result.Failure(ImageError.ReadFailed)
            val viewModel = buildViewModel()
            viewModel.onEvent(AnalysisUiEvent.SelectImage)
            advanceUntilIdle()

            // When
            viewModel.onEvent(AnalysisUiEvent.DismissError)

            // Then
            assertNull(viewModel.uiState.value.error)
        }
}
