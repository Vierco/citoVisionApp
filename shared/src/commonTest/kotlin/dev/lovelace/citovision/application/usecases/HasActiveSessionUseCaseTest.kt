package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.SessionRepository
import dev.lovelace.citovision.domain.entities.AuthUser
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HasActiveSessionUseCaseTest {
    private val authService = mock<AuthService>()
    private val sessionRepository = mock<SessionRepository>()
    private val useCase = HasActiveSessionUseCase(authService, sessionRepository)

    @Test
    fun `given an account user when checking then returns true`() =
        runTest {
            // Given
            every { authService.currentUser } returns
                flowOf(AuthUser(uid = "1", email = "a@a.com", displayName = null, isGuest = false))

            // When / Then
            assertTrue(useCase())
        }

    @Test
    fun `given no account but persisted guest flag when checking then returns true`() =
        runTest {
            // Given
            every { authService.currentUser } returns flowOf(null)
            every { sessionRepository.isGuestSession() } returns flowOf(true)

            // When / Then
            assertTrue(useCase())
        }

    @Test
    fun `given no session when checking then returns false`() =
        runTest {
            // Given
            every { authService.currentUser } returns flowOf(null)
            every { sessionRepository.isGuestSession() } returns flowOf(false)

            // When / Then
            assertFalse(useCase())
        }
}
