package dev.lovelace.citovision.presentation.viewmodels

import dev.lovelace.citovision.application.ports.AnalysisRepository
import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.RemoteFeedback
import dev.lovelace.citovision.application.ports.SessionRepository
import dev.lovelace.citovision.application.usecases.DeleteAllAnalysesUseCase
import dev.lovelace.citovision.application.usecases.ObserveCurrentUserUseCase
import dev.lovelace.citovision.application.usecases.ObserveSessionStatusUseCase
import dev.lovelace.citovision.application.usecases.SessionStatus
import dev.lovelace.citovision.application.usecases.SignOutUseCase
import dev.lovelace.citovision.application.usecases.SubmitFeedbackUseCase
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.AuthUser
import dev.lovelace.citovision.presentation.events.SettingsUiEvent
import dev.lovelace.citovision.presentation.navigation.NavigationEvent
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
 * Mokkery no puede mockear clases final (los use cases), así que se construyen los use cases reales
 * con los puertos (interfaces) mockeados.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val authService = mock<AuthService>()
    private val sessionRepository = mock<SessionRepository>()
    private val analysisRepository = mock<AnalysisRepository>()
    private val remoteFeedback = mock<RemoteFeedback>()
    private val signOut = SignOutUseCase(authService, sessionRepository)
    private val deleteAllAnalyses = DeleteAllAnalysesUseCase(analysisRepository)
    private val submitFeedback = SubmitFeedbackUseCase(remoteFeedback, authService)
    private val observeSessionStatus = ObserveSessionStatusUseCase(authService, sessionRepository)
    private val observeCurrentUser = ObserveCurrentUserUseCase(authService)
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() =
        SettingsViewModel(
            signOut = signOut,
            deleteAllAnalyses = deleteAllAnalyses,
            submitFeedback = submitFeedback,
            observeSessionStatus = observeSessionStatus,
            observeCurrentUser = observeCurrentUser,
        )

    private fun buildViewModel(): SettingsViewModel {
        // El estado de sesión se construye al instanciar el ViewModel (combine de ambos puertos).
        every { authService.currentUser } returns flowOf(null)
        every { sessionRepository.isGuestSession() } returns flowOf(true)
        return newViewModel()
    }

    @Test
    fun `given sign-out event when handled then signs out and navigates to Login`() =
        runTest(dispatcher) {
            // Given
            everySuspend { sessionRepository.setGuestSession(false) } returns Unit
            everySuspend { authService.signOut() } returns Result.Success(Unit)
            val viewModel = buildViewModel()

            // When
            viewModel.onEvent(SettingsUiEvent.SignOut)

            // Then
            assertEquals(NavigationEvent.ToLogin, viewModel.navigationEvents.first())
            verifySuspend { sessionRepository.setGuestSession(false) }
            verifySuspend { authService.signOut() }
        }

    @Test
    fun `given login event when handled then navigates to Login`() =
        runTest(dispatcher) {
            // Given
            val viewModel = buildViewModel()

            // When
            viewModel.onEvent(SettingsUiEvent.Login)

            // Then
            assertEquals(NavigationEvent.ToLogin, viewModel.navigationEvents.first())
        }

    @Test
    fun `given account with email login when observed then exposes email without avatar`() =
        runTest(dispatcher) {
            // Given
            every {
                authService.currentUser
            } returns flowOf(AuthUser("u1", "doc@clinica.com", null, isGuest = false))
            every { sessionRepository.isGuestSession() } returns flowOf(false)
            val viewModel = newViewModel()

            // When
            val state = viewModel.uiState.first { it.sessionStatus == SessionStatus.ACCOUNT }

            // Then
            assertEquals("doc@clinica.com", state.email)
            assertNull(state.avatarUrl)
        }

    @Test
    fun `given account with google avatar when observed then exposes avatar url`() =
        runTest(dispatcher) {
            // Given
            every {
                authService.currentUser
            } returns
                flowOf(
                    AuthUser("u1", "doc@gmail.com", "Doc", isGuest = false, photoUrl = "https://avatar/u1"),
                )
            every { sessionRepository.isGuestSession() } returns flowOf(false)
            val viewModel = newViewModel()

            // When
            val state = viewModel.uiState.first { it.sessionStatus == SessionStatus.ACCOUNT }

            // Then
            assertEquals("https://avatar/u1", state.avatarUrl)
        }

    @Test
    fun `given clear-local-analyses event when handled then deletes all and shows confirmation`() =
        runTest(dispatcher) {
            // Given
            everySuspend { analysisRepository.deleteAllAnalyses() } returns Result.Success(Unit)
            val viewModel = buildViewModel()

            // When
            viewModel.onEvent(SettingsUiEvent.ClearLocalAnalyses)

            // Then
            assertTrue(viewModel.uiState.first { it.clearedConfirmationVisible }.clearedConfirmationVisible)
            verifySuspend { analysisRepository.deleteAllAnalyses() }
        }

    @Test
    fun `given valid feedback when submitting then it is sent and confirmation is shown`() =
        runTest(dispatcher) {
            // Given
            every { authService.currentUser } returns flowOf(null)
            every { sessionRepository.isGuestSession() } returns flowOf(true)
            everySuspend { remoteFeedback.submit(any()) } returns Result.Success(Unit)
            val viewModel = newViewModel()

            // When
            viewModel.onEvent(SettingsUiEvent.OpenFeedback)
            viewModel.onEvent(SettingsUiEvent.FeedbackEmailChanged("doc@clinica.com"))
            viewModel.onEvent(SettingsUiEvent.FeedbackMessageChanged("Muy buena app"))
            viewModel.onEvent(SettingsUiEvent.SubmitFeedback)

            // Then
            val state = viewModel.uiState.first { it.feedbackSentVisible }
            assertFalse(state.feedbackDialogVisible)
            verifySuspend { remoteFeedback.submit(any()) }
        }
}
