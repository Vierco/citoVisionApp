package dev.lovelace.citovision.presentation.viewmodels

import dev.lovelace.citovision.application.ports.AnalysisRepository
import dev.lovelace.citovision.application.usecases.DeleteAnalysisUseCase
import dev.lovelace.citovision.application.usecases.ObserveAnalysesUseCase
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.Analysis
import dev.lovelace.citovision.domain.errors.AnalysisError
import dev.lovelace.citovision.presentation.events.HistoryUiEvent
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
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
import kotlin.time.Instant

/**
 * Mokkery no puede mockear clases final (los use cases), así que se construyen los use cases reales con el
 * puerto [AnalysisRepository] mockeado.
 */
class HistoryViewModelTest {
    private val analysisRepository = mock<AnalysisRepository>()
    private val observeAnalyses = ObserveAnalysesUseCase(analysisRepository)
    private val deleteAnalysis = DeleteAnalysisUseCase(analysisRepository)
    private val dispatcher = StandardTestDispatcher()

    private fun analysis(id: String) =
        Analysis(
            id = id,
            patient = "PAC-2026-00$id",
            performedAt = Instant.fromEpochMilliseconds(1_700_000_000_000),
            summary = "Resumen",
            imagePath = "/tmp/$id.png",
            cellCounts = emptyList(),
        )

    private fun buildViewModel() = HistoryViewModel(observeAnalyses, deleteAnalysis)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given no analyses when observing then the state is empty`() =
        runTest(dispatcher) {
            // Given
            every { analysisRepository.observeAnalyses() } returns flowOf(emptyList())

            // When
            val viewModel = buildViewModel()
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isEmpty)
            assertFalse(state.isLoading)
        }

    @Test
    fun `given stored analyses when observing then they are exposed in the state`() =
        runTest(dispatcher) {
            // Given
            val analyses = listOf(analysis("1"), analysis("2"))
            every { analysisRepository.observeAnalyses() } returns flowOf(analyses)

            // When
            val viewModel = buildViewModel()
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertEquals(analyses, state.analyses)
            assertFalse(state.isEmpty)
        }

    @Test
    fun `given a long press when requesting delete then asks for confirmation without deleting`() =
        runTest(dispatcher) {
            // Given
            every { analysisRepository.observeAnalyses() } returns flowOf(listOf(analysis("1")))
            val viewModel = buildViewModel()
            advanceUntilIdle()

            // When
            viewModel.onEvent(HistoryUiEvent.RequestDelete(analysis("1")))

            // Then
            assertEquals(
                "1",
                viewModel.uiState.value.pendingDeletion
                    ?.id,
            )
        }

    @Test
    fun `given a pending deletion when confirming then the analysis is deleted`() =
        runTest(dispatcher) {
            // Given
            every { analysisRepository.observeAnalyses() } returns flowOf(listOf(analysis("1")))
            everySuspend { analysisRepository.deleteAnalysis("1") } returns Result.Success(Unit)
            val viewModel = buildViewModel()
            advanceUntilIdle()
            viewModel.onEvent(HistoryUiEvent.RequestDelete(analysis("1")))

            // When
            viewModel.onEvent(HistoryUiEvent.ConfirmDelete)
            advanceUntilIdle()

            // Then
            verifySuspend { analysisRepository.deleteAnalysis("1") }
            assertNull(viewModel.uiState.value.pendingDeletion)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `given a pending deletion when cancelling then nothing is deleted`() =
        runTest(dispatcher) {
            // Given
            every { analysisRepository.observeAnalyses() } returns flowOf(listOf(analysis("1")))
            val viewModel = buildViewModel()
            advanceUntilIdle()
            viewModel.onEvent(HistoryUiEvent.RequestDelete(analysis("1")))

            // When
            viewModel.onEvent(HistoryUiEvent.CancelDelete)
            advanceUntilIdle()

            // Then
            assertNull(viewModel.uiState.value.pendingDeletion)
        }

    @Test
    fun `given deletion fails when confirming then an error is shown`() =
        runTest(dispatcher) {
            // Given
            every { analysisRepository.observeAnalyses() } returns flowOf(listOf(analysis("1")))
            everySuspend { analysisRepository.deleteAnalysis("1") } returns
                Result.Failure(AnalysisError.StorageFailure)
            val viewModel = buildViewModel()
            advanceUntilIdle()
            viewModel.onEvent(HistoryUiEvent.RequestDelete(analysis("1")))

            // When
            viewModel.onEvent(HistoryUiEvent.ConfirmDelete)
            advanceUntilIdle()

            // Then
            assertTrue(viewModel.uiState.value.error != null)
        }

    @Test
    fun `given an analysis when showing detail then it is exposed and can be dismissed`() =
        runTest(dispatcher) {
            // Given
            every { analysisRepository.observeAnalyses() } returns flowOf(listOf(analysis("1")))
            val viewModel = buildViewModel()
            advanceUntilIdle()

            // When
            viewModel.onEvent(HistoryUiEvent.ShowDetail(analysis("1")))

            // Then
            assertEquals(
                "1",
                viewModel.uiState.value.detail
                    ?.id,
            )

            // When
            viewModel.onEvent(HistoryUiEvent.DismissDetail)

            // Then
            assertNull(viewModel.uiState.value.detail)
        }
}
