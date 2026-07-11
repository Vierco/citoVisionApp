package dev.lovelace.citovision.presentation.viewmodels

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.SessionRepository
import dev.lovelace.citovision.application.usecases.HasActiveSessionUseCase
import dev.lovelace.citovision.domain.entities.AuthUser
import dev.lovelace.citovision.presentation.navigation.NavigationEvent
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
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

/**
 * Mokkery no puede mockear clases final (los use cases), así que se construye el use case real
 * con los puertos (interfaces) mockeados.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {
    private val authService = mock<AuthService>()
    private val sessionRepository = mock<SessionRepository>()
    private val hasActiveSession = HasActiveSessionUseCase(authService, sessionRepository)
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given an active account session when starting then navigates to Main`() =
        runTest(dispatcher) {
            // Given
            every { authService.currentUser } returns
                flowOf(AuthUser(uid = "1", email = "a@a.com", displayName = null, isGuest = false))

            // When
            val viewModel = SplashViewModel(hasActiveSession)

            // Then
            assertEquals(NavigationEvent.ToMain, viewModel.navigationEvents.first())
        }

    @Test
    fun `given no active session when starting then navigates to Login`() =
        runTest(dispatcher) {
            // Given
            every { authService.currentUser } returns flowOf(null)
            every { sessionRepository.isGuestSession() } returns flowOf(false)

            // When
            val viewModel = SplashViewModel(hasActiveSession)

            // Then
            assertEquals(NavigationEvent.ToLogin, viewModel.navigationEvents.first())
        }
}
