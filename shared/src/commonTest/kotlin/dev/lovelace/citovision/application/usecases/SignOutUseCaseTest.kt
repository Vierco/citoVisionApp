package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.PatientCodeRepository
import dev.lovelace.citovision.application.ports.SessionRepository
import dev.lovelace.citovision.core.result.Result
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SignOutUseCaseTest {
    private val authService = mock<AuthService>()
    private val sessionRepository = mock<SessionRepository>()
    private val patientCodeRepository = mock<PatientCodeRepository>()
    private val useCase = SignOutUseCase(authService, sessionRepository, patientCodeRepository)

    @Test
    fun `given a session when signing out then clears guest flag and signs out from firebase`() =
        runTest {
            // Given
            everySuspend { sessionRepository.setGuestSession(false) } returns Unit
            everySuspend { patientCodeRepository.clear() } returns Unit
            everySuspend { authService.signOut() } returns Result.Success(Unit)

            // When
            val result = useCase()

            // Then
            assertEquals(Result.Success(Unit), result)
            verifySuspend { sessionRepository.setGuestSession(false) }
            verifySuspend { patientCodeRepository.clear() }
            verifySuspend { authService.signOut() }
        }
}
