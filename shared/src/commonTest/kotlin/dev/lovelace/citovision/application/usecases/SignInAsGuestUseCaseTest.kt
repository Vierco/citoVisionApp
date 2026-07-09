package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.SessionRepository
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.AuthUser
import dev.lovelace.citovision.domain.errors.AuthError
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SignInAsGuestUseCaseTest {
    private val authService = mock<AuthService>()
    private val sessionRepository = mock<SessionRepository>()
    private val useCase = SignInAsGuestUseCase(authService, sessionRepository)

    @Test
    fun `given guest sign-in succeeds when invoking then persists guest flag`() =
        runTest {
            // Given
            val guest = AuthUser(uid = "guest", email = null, displayName = null, isGuest = true)
            everySuspend { authService.signInAsGuest() } returns Result.Success(guest)
            everySuspend { sessionRepository.setGuestSession(true) } returns Unit

            // When
            val result = useCase()

            // Then
            assertEquals(Result.Success(guest), result)
            verifySuspend { sessionRepository.setGuestSession(true) }
        }

    @Test
    fun `given guest sign-in fails when invoking then returns the failure`() =
        runTest {
            // Given
            everySuspend { authService.signInAsGuest() } returns Result.Failure(AuthError.Unknown("boom"))

            // When
            val result = useCase()

            // Then
            assertEquals(Result.Failure(AuthError.Unknown("boom")), result)
        }
}
