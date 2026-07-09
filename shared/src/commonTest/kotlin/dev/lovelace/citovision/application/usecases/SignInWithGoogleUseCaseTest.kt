package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.GoogleSignInLauncher
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

/**
 * Orquestación del login con Google: el use case resuelve el `idToken` con el flujo nativo
 * (puerto [GoogleSignInLauncher]) y lo canjea en [AuthService]. Ambos puertos son interfaces mockeables.
 */
class SignInWithGoogleUseCaseTest {
    private val authService = mock<AuthService>()
    private val googleSignInLauncher = mock<GoogleSignInLauncher>()
    private val useCase = SignInWithGoogleUseCase(authService, googleSignInLauncher)

    @Test
    fun `given a valid idToken when invoking then exchanges it in the auth service`() =
        runTest {
            // Given
            val user = AuthUser(uid = "1", email = "a@a.com", displayName = "Ada", isGuest = false)
            everySuspend { googleSignInLauncher.requestIdToken() } returns Result.Success("id-token")
            everySuspend { authService.signInWithGoogle("id-token") } returns Result.Success(user)

            // When
            val result = useCase()

            // Then
            assertEquals(Result.Success(user), result)
            verifySuspend { authService.signInWithGoogle("id-token") }
        }

    @Test
    fun `given the native flow is cancelled when invoking then propagates the failure`() =
        runTest {
            // Given
            everySuspend { googleSignInLauncher.requestIdToken() } returns
                Result.Failure(AuthError.GoogleSignInCancelled)

            // When
            val result = useCase()

            // Then
            assertEquals(Result.Failure(AuthError.GoogleSignInCancelled), result)
        }
}
