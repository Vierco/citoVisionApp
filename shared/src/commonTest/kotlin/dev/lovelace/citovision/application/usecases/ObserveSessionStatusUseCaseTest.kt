package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.SessionRepository
import dev.lovelace.citovision.domain.entities.AuthUser
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveSessionStatusUseCaseTest {
    private val authService = mock<AuthService>()
    private val sessionRepository = mock<SessionRepository>()
    private val useCase = ObserveSessionStatusUseCase(authService, sessionRepository)

    @Test
    fun `given an authenticated account when observing then emits ACCOUNT`() =
        runTest {
            // Given
            every { authService.currentUser } returns
                flowOf(AuthUser(uid = "1", email = "a@a.com", displayName = null, isGuest = false))
            every { sessionRepository.isGuestSession() } returns flowOf(false)

            // When / Then
            assertEquals(SessionStatus.ACCOUNT, useCase().first())
        }

    @Test
    fun `given a local guest user when observing then emits GUEST`() =
        runTest {
            // Given: en Desktop el invitado aparece como currentUser con isGuest=true
            every { authService.currentUser } returns
                flowOf(AuthUser(uid = "guest", email = null, displayName = null, isGuest = true))
            every { sessionRepository.isGuestSession() } returns flowOf(false)

            // When / Then
            assertEquals(SessionStatus.GUEST, useCase().first())
        }

    @Test
    fun `given only the persisted guest flag when observing then emits GUEST`() =
        runTest {
            // Given: en Android el invitado es local (currentUser null) y solo consta en el flag
            every { authService.currentUser } returns flowOf(null)
            every { sessionRepository.isGuestSession() } returns flowOf(true)

            // When / Then
            assertEquals(SessionStatus.GUEST, useCase().first())
        }

    @Test
    fun `given no session when observing then emits NONE`() =
        runTest {
            // Given
            every { authService.currentUser } returns flowOf(null)
            every { sessionRepository.isGuestSession() } returns flowOf(false)

            // When / Then
            assertEquals(SessionStatus.NONE, useCase().first())
        }
}
