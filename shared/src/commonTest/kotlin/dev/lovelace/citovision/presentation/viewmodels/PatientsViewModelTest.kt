package dev.lovelace.citovision.presentation.viewmodels

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.RemotePatientAnalyses
import dev.lovelace.citovision.application.usecases.DeleteRemoteAnalysisUseCase
import dev.lovelace.citovision.application.usecases.ListPatientCodesUseCase
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
    private val listCodes = ListPatientCodesUseCase(authService, remote)
    private val dispatcher = StandardTestDispatcher()

    private val account = AuthUser("u1", "a@b.com", null, isGuest = false)

    private fun buildViewModel() = PatientsViewModel(search, deleteRemote, listCodes)

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
    fun `given letters when typing then they are stripped`() =
        runTest(dispatcher) {
            val viewModel = buildViewModel()

            viewModel.onEvent(PatientsUiEvent.QueryChanged("12-ab34"))

            assertEquals("12-34", viewModel.uiState.value.query)
        }

    @Test
    fun `given an account when loading codes then shows the patient list`() =
        runTest(dispatcher) {
            every { authService.currentUser } returns flowOf(account)
            everySuspend { remote.queryPatientCodes("u1") } returns Result.Success(listOf("12-34", "20-26"))
            val viewModel = buildViewModel()

            viewModel.onEvent(PatientsUiEvent.LoadCodes)
            advanceUntilIdle()

            assertEquals(listOf("12-34", "20-26"), viewModel.uiState.value.patientCodes)
            assertFalse(viewModel.uiState.value.isCodesLoading)
        }

    @Test
    fun `given a loaded list when typing then only the matching codes remain`() =
        runTest(dispatcher) {
            every { authService.currentUser } returns flowOf(account)
            everySuspend { remote.queryPatientCodes("u1") } returns Result.Success(listOf("12-34", "20-26"))
            val viewModel = buildViewModel()
            viewModel.onEvent(PatientsUiEvent.LoadCodes)
            advanceUntilIdle()

            viewModel.onEvent(PatientsUiEvent.QueryChanged("20"))

            assertEquals(listOf("20-26"), viewModel.uiState.value.filteredCodes)
        }

    @Test
    fun `given a guest when loading codes then requires an account`() =
        runTest(dispatcher) {
            every { authService.currentUser } returns flowOf(AuthUser("guest", null, null, isGuest = true))
            val viewModel = buildViewModel()

            viewModel.onEvent(PatientsUiEvent.LoadCodes)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.requiresAccount)
            assertTrue(
                viewModel.uiState.value.patientCodes
                    .isEmpty(),
            )
        }

    @Test
    fun `given a network failure when loading codes then shows the list error`() =
        runTest(dispatcher) {
            every { authService.currentUser } returns flowOf(account)
            everySuspend { remote.queryPatientCodes("u1") } returns Result.Failure(RemoteAnalysisError.Network)
            val viewModel = buildViewModel()

            viewModel.onEvent(PatientsUiEvent.LoadCodes)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.codesErrorVisible)
        }

    @Test
    fun `given a code from the list when selecting it then shows the results view`() =
        runTest(dispatcher) {
            every { authService.currentUser } returns flowOf(account)
            everySuspend { remote.queryByPatient("u1", "12-34") } returns Result.Success(listOf(analysis()))
            val viewModel = buildViewModel()

            viewModel.onEvent(PatientsUiEvent.SelectCode("12-34"))
            advanceUntilIdle()

            assertEquals("12-34", viewModel.uiState.value.resultsPatientCode)
            assertEquals(1, viewModel.uiState.value.results.size)
        }

    @Test
    fun `given a filter with one survivor when submitting from the keyboard then opens that patient`() =
        runTest(dispatcher) {
            every { authService.currentUser } returns flowOf(account)
            everySuspend { remote.queryPatientCodes("u1") } returns Result.Success(listOf("12-34", "20-26"))
            everySuspend { remote.queryByPatient("u1", "20-26") } returns Result.Success(listOf(analysis()))
            val viewModel = buildViewModel()
            viewModel.onEvent(PatientsUiEvent.LoadCodes)
            advanceUntilIdle()
            viewModel.onEvent(PatientsUiEvent.QueryChanged("20"))

            viewModel.onEvent(PatientsUiEvent.SubmitQuery)
            advanceUntilIdle()

            assertEquals("20-26", viewModel.uiState.value.resultsPatientCode)
        }

    @Test
    fun `given a filter with several survivors when submitting from the keyboard then nothing happens`() =
        runTest(dispatcher) {
            every { authService.currentUser } returns flowOf(account)
            everySuspend { remote.queryPatientCodes("u1") } returns Result.Success(listOf("20-25", "20-26"))
            val viewModel = buildViewModel()
            viewModel.onEvent(PatientsUiEvent.LoadCodes)
            advanceUntilIdle()
            viewModel.onEvent(PatientsUiEvent.QueryChanged("20"))

            viewModel.onEvent(PatientsUiEvent.SubmitQuery)
            advanceUntilIdle()

            assertEquals(null, viewModel.uiState.value.resultsPatientCode)
        }

    @Test
    fun `given results when confirming delete then removes the card`() =
        runTest(dispatcher) {
            every { authService.currentUser } returns flowOf(account)
            everySuspend { remote.queryByPatient("u1", "12-34") } returns Result.Success(listOf(analysis()))
            everySuspend { remote.deleteAnalysis("a1") } returns Result.Success(Unit)
            val viewModel = buildViewModel()
            viewModel.onEvent(PatientsUiEvent.SelectCode("12-34"))
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
            every { authService.currentUser } returns flowOf(account)
            val second = Analysis("a2", "12-34", Instant.fromEpochMilliseconds(2), "s2", null, emptyList())
            val fakeRemote = QueueRemote(mutableListOf(listOf(analysis()), listOf(analysis(), second)))
            val viewModel =
                PatientsViewModel(
                    SearchPatientAnalysesUseCase(authService, fakeRemote),
                    DeleteRemoteAnalysisUseCase(fakeRemote),
                    ListPatientCodesUseCase(authService, fakeRemote),
                )
            viewModel.onEvent(PatientsUiEvent.SelectCode("12-34"))
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

        override suspend fun queryPatientCodes(ownerUid: String): Result<List<String>, RemoteAnalysisError> =
            Result.Success(listOf("12-34"))

        override suspend fun deleteAnalysis(analysisId: String): Result<Unit, RemoteAnalysisError> =
            Result.Success(Unit)
    }
}
