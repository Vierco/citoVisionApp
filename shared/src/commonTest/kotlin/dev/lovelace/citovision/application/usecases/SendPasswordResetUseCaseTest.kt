package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.errors.AuthError
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * El use case delega en el puerto [AuthService]. La regla anti-enumeración (SPEC-0002 RN-2: tratar
 * `UserNotFound` como éxito) vive en el LoginViewModel, no aquí: aquí el fallo se propaga tal cual.
 */
class SendPasswordResetUseCaseTest {
    private val authService = mock<AuthService>()
    private val useCase = SendPasswordResetUseCase(authService)

    @Test
    fun `given the reset email is sent when invoking then returns success`() =
        runTest {
            // Given
            everySuspend { authService.sendPasswordReset("a@a.com") } returns Result.Success(Unit)

            // When
            val result = useCase("a@a.com")

            // Then
            assertEquals(Result.Success(Unit), result)
            verifySuspend { authService.sendPasswordReset("a@a.com") }
        }

    @Test
    fun `given the auth service fails when invoking then propagates the failure`() =
        runTest {
            // Given
            everySuspend { authService.sendPasswordReset("a@a.com") } returns
                Result.Failure(AuthError.UserNotFound)

            // When
            val result = useCase("a@a.com")

            // Then
            assertEquals(Result.Failure(AuthError.UserNotFound), result)
        }
}
