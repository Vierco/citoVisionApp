package dev.lovelace.citovision.presentation.viewmodels

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.SessionRepository
import dev.lovelace.citovision.application.usecases.ObserveSessionStatusUseCase
import dev.lovelace.citovision.application.usecases.SignOutUseCase
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.presentation.events.SettingsUiEvent
import dev.lovelace.citovision.presentation.navigation.NavigationEvent
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
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

/**
 * Mokkery no puede mockear clases final (los use cases), así que se construyen los use cases reales
 * con los puertos (interfaces) mockeados.
 */
class SettingsViewModelTest {
    private val authService = mock<AuthService>()
    private val sessionRepository = mock<SessionRepository>()
    private val signOut = SignOutUseCase(authService, sessionRepository)
    private val observeSessionStatus = ObserveSessionStatusUseCase(authService, sessionRepository)
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): SettingsViewModel {
        // El estado de sesión se construye al instanciar el ViewModel (combine de ambos puertos).
        every { authService.currentUser } returns flowOf(null)
        every { sessionRepository.isGuestSession() } returns flowOf(true)
        return SettingsViewModel(signOut, observeSessionStatus)
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
}
