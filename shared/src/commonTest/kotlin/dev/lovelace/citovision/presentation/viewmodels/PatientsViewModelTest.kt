package dev.lovelace.citovision.presentation.viewmodels

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.RemotePatientAnalyses
import dev.lovelace.citovision.application.usecases.DeleteRemoteAnalysisUseCase
import dev.lovelace.citovision.application.usecases.SearchPatientAnalysesUseCase
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.Analysis
import dev.lovelace.citovision.domain.entities.AuthUser
import dev.lovelace.citovision.domain.errors.RemoteAnalysisError
import dev.lovelace.citovision.presentation.events.PatientsUiEvent
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Mokkery no puede mockear el use case (clase final): se construye el real con los puertos mockeados.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class PatientsViewModelTest {
    private val authService = mock<AuthService>()
    private val remote = mock<RemotePatientAnalyses>()
    private val search = SearchPatientAnalysesUseCase(authService, remote)
    private val deleteRemote = DeleteRemoteAnalysisUseCase(remote)
    private val dispatcher = StandardTestDispatcher()

    private fun buildViewModel() = PatientsViewModel(search, deleteRemote)

    private fun analysis() = Analysis("a1", "12-34", Instant.fromEpochMilliseconds(1), "s", null, emptyList())

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given an invalid code when typing then search stays disabled`() =
        runTest(dispatcher) {
            val viewModel = buildViewModel()

            viewModel.onEvent(PatientsUiEvent.QueryChanged("12"))

            assertFalse(viewModel.uiState.value.isQueryValid)
        }

    @Test
    fun `given a valid code when typing then search is enabled`() =
        runTest(dispatcher) {
            val viewModel = buildViewModel()

            viewModel.onEvent(PatientsUiEvent.QueryChanged("12-34"))

            assertTrue(viewModel.uiState.value.isQueryValid)
        }

    @Test
    fun `given letters when typing then they are stripped`() =
        runTest(dispatcher) {
            val viewModel = buildViewModel()

            viewModel.onEvent(PatientsUiEvent.QueryChanged("12-ab34"))

            assertEquals("12-34", viewModel.uiState.value.query)
        }

    @Test
    fun `given an account with results when searching then shows the results view`() =
        runTest(dispatcher) {
            every { authService.currentUser } returns flowOf(AuthUser("u1", "a@b.com", null, isGuest = false))
            everySuspend { remote.queryByPatient("u1", "12-34") } returns Result.Success(listOf(analysis()))
            val viewModel = buildViewModel()
            viewModel.onEvent(PatientsUiEvent.QueryChanged("12-34"))

            viewModel.onEvent(PatientsUiEvent.Search)
            advanceUntilIdle()

            assertEquals("12-34", viewModel.uiState.value.resultsPatientCode)
            assertEquals(1, viewModel.uiState.value.results.size)
        }

    @Test
    fun `given a guest when searching then shows requires account`() =
        runTest(dispatcher) {
            every { authService.currentUser } returns flowOf(AuthUser("guest", null, null, isGuest = true))
            val viewModel = buildViewModel()
            viewModel.onEvent(PatientsUiEvent.QueryChanged("12-34"))

            viewModel.onEvent(PatientsUiEvent.Search)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.requiresAccountVisible)
        }

    @Test
    fun `given results when confirming delete then removes the card`() =
        runTest(dispatcher) {
            every { authService.currentUser } returns flowOf(AuthUser("u1", "a@b.com", null, isGuest = false))
            everySuspend { remote.queryByPatient("u1", "12-34") } returns Result.Success(listOf(analysis()))
            everySuspend { remote.deleteAnalysis("a1") } returns Result.Success(Unit)
            val viewModel = buildViewModel()
            viewModel.onEvent(PatientsUiEvent.QueryChanged("12-34"))
            viewModel.onEvent(PatientsUiEvent.Search)
            advanceUntilIdle()

            viewModel.onEvent(PatientsUiEvent.RequestDelete(analysis()))
            viewModel.onEvent(PatientsUiEvent.ConfirmDelete)
            advanceUntilIdle()

            assertTrue(
                viewModel.uiState.value.results
                    .isEmpty(),
            )
        }

    @Test
    fun `given a shown patient when refreshing then reloads entries from remote`() =
        runTest(dispatcher) {
            every { authService.currentUser } returns flowOf(AuthUser("u1", "a@b.com", null, isGuest = false))
            val second = Analysis("a2", "12-34", Instant.fromEpochMilliseconds(2), "s2", null, emptyList())
            val fakeRemote = QueueRemote(mutableListOf(listOf(analysis()), listOf(analysis(), second)))
            val viewModel =
                PatientsViewModel(
                    SearchPatientAnalysesUseCase(authService, fakeRemote),
                    DeleteRemoteAnalysisUseCase(fakeRemote),
                )
            viewModel.onEvent(PatientsUiEvent.QueryChanged("12-34"))
            viewModel.onEvent(PatientsUiEvent.Search)
            advanceUntilIdle()
            assertEquals(1, viewModel.uiState.value.results.size)

            viewModel.onEvent(PatientsUiEvent.Refresh)
            advanceUntilIdle()

            assertEquals(2, viewModel.uiState.value.results.size)
            assertEquals("12-34", viewModel.uiState.value.resultsPatientCode)
        }

    /** Fake que devuelve una página de resultados por llamada, para verificar la recarga. */
    private class QueueRemote(
        private val pages: MutableList<List<Analysis>>,
    ) : RemotePatientAnalyses {
        override suspend fun queryByPatient(
            ownerUid: String,
            patientCode: String,
        ): Result<List<Analysis>, RemoteAnalysisError> = Result.Success(pages.removeAt(0))

        override suspend fun deleteAnalysis(analysisId: String): Result<Unit, RemoteAnalysisError> =
            Result.Success(Unit)
    }
}
