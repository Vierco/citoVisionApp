package dev.lovelace.citovision.presentation.viewmodels

import dev.lovelace.citovision.application.ports.AnalysisImageStore
import dev.lovelace.citovision.application.ports.AnalysisRepository
import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.CellDetector
import dev.lovelace.citovision.application.ports.ImagePicker
import dev.lovelace.citovision.application.ports.PatientCodeRepository
import dev.lovelace.citovision.application.ports.RemoteAnalysisSync
import dev.lovelace.citovision.application.usecases.AnalyzeSampleUseCase
import dev.lovelace.citovision.application.usecases.ObserveLastPatientCodeUseCase
import dev.lovelace.citovision.application.usecases.PickImageUseCase
import dev.lovelace.citovision.application.usecases.ProcessPendingSyncUseCase
import dev.lovelace.citovision.application.usecases.SaveLastPatientCodeUseCase
import dev.lovelace.citovision.application.usecases.SyncAnalysisUseCase
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.BoundingBox
import dev.lovelace.citovision.domain.entities.CellClass
import dev.lovelace.citovision.domain.entities.Detection
import dev.lovelace.citovision.domain.entities.SelectedImage
import dev.lovelace.citovision.domain.errors.ImageError
import dev.lovelace.citovision.domain.errors.InferenceError
import dev.lovelace.citovision.presentation.events.AnalysisUiEvent
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisViewModelTest {
    private val imagePicker = mock<ImagePicker>()
    private val cellDetector = mock<CellDetector>()
    private val analysisRepository = mock<AnalysisRepository>()
    private val analysisImageStore = mock<AnalysisImageStore>()
    private val authService = mock<AuthService>()
    private val remoteAnalysisSync = mock<RemoteAnalysisSync>()
    private val patientCodeRepository = mock<PatientCodeRepository>()
    private val pickImage = PickImageUseCase(imagePicker)
    private val analyzeSample = AnalyzeSampleUseCase(cellDetector, analysisRepository, analysisImageStore)
    private val syncAnalysis = SyncAnalysisUseCase(authService, remoteAnalysisSync)
    private val processPendingSync = ProcessPendingSyncUseCase(remoteAnalysisSync)
    private val observeLastPatientCode = ObserveLastPatientCodeUseCase(patientCodeRepository)
    private val saveLastPatientCode = SaveLastPatientCodeUseCase(patientCodeRepository)
    private val dispatcher = StandardTestDispatcher()

    private fun buildViewModel() =
        AnalysisViewModel(
            pickImage,
            analyzeSample,
            syncAnalysis,
            processPendingSync,
            observeLastPatientCode,
            saveLastPatientCode,
        )

    private val validImage =
        SelectedImage(bytes = ByteArray(4), fileName = "muestra.png", mimeType = "image/png", sizeBytes = 4)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        everySuspend { remoteAnalysisSync.processPending() } returns Result.Success(Unit)
        every { patientCodeRepository.lastPatientCode() } returns flowOf("")
        everySuspend { patientCodeRepository.setLastPatientCode(any()) } returns Unit
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

    @Test
    fun `given only non-cell detections when confirming scan then shows the no-cells popup`() =
        runTest(dispatcher) {
            // Given
            everySuspend { imagePicker.pickImage() } returns Result.Success(validImage)
            everySuspend { cellDetector.detect(any()) } returns
                Result.Success(listOf(detection(CellClass.ARTEFACTO)))
            val viewModel = readyToScanViewModel()

            // When
            viewModel.onEvent(AnalysisUiEvent.ConfirmScan)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.noCellsVisible)
            assertFalse(state.isSaving)
        }

    @Test
    fun `given inference fails when confirming scan then shows the inference error popup`() =
        runTest(dispatcher) {
            // Given
            everySuspend { imagePicker.pickImage() } returns Result.Success(validImage)
            everySuspend { cellDetector.detect(any()) } returns Result.Failure(InferenceError.InferenceFailed)
            val viewModel = readyToScanViewModel()

            // When
            viewModel.onEvent(AnalysisUiEvent.ConfirmScan)
            advanceUntilIdle()

            // Then
            assertTrue(viewModel.uiState.value.inferenceErrorVisible)
        }

    /** Deja el ViewModel con imagen seleccionada y un código de paciente válido, listo para confirmar. */
    private fun TestScope.readyToScanViewModel(): AnalysisViewModel {
        val viewModel = buildViewModel()
        viewModel.onEvent(AnalysisUiEvent.SelectImage)
        advanceUntilIdle()
        viewModel.onEvent(AnalysisUiEvent.StartScan)
        viewModel.onEvent(AnalysisUiEvent.PatientCodeChanged("12-34"))
        return viewModel
    }

    private fun detection(cellClass: CellClass): Detection =
        Detection(cellClass = cellClass, confidence = 0.9f, box = BoundingBox(0f, 0f, 1f, 1f))
}
