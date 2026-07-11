package dev.lovelace.citovision.presentation.viewmodels

import citovision.shared.generated.resources.Res
import citovision.shared.generated.resources.login_error_email_format
import citovision.shared.generated.resources.login_error_password_length
import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.GoogleSignInLauncher
import dev.lovelace.citovision.application.ports.SessionRepository
import dev.lovelace.citovision.application.usecases.SendPasswordResetUseCase
import dev.lovelace.citovision.application.usecases.SignInAsGuestUseCase
import dev.lovelace.citovision.application.usecases.SignInWithEmailUseCase
import dev.lovelace.citovision.application.usecases.SignInWithGoogleUseCase
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.AuthUser
import dev.lovelace.citovision.domain.errors.AuthError
import dev.lovelace.citovision.presentation.events.LoginUiEvent
import dev.lovelace.citovision.presentation.navigation.NavigationEvent
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mokkery no puede mockear clases final (los use cases), así que se construyen los use cases reales
 * con los puertos (interfaces) mockeados. La validación de formato de campos vive en el ViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val authService = mock<AuthService>()
    private val googleSignInLauncher = mock<GoogleSignInLauncher>()
    private val sessionRepository = mock<SessionRepository>()
    private val dispatcher = StandardTestDispatcher()

    private fun buildViewModel() =
        LoginViewModel(
            signInWithEmail = SignInWithEmailUseCase(authService),
            signInWithGoogle = SignInWithGoogleUseCase(authService, googleSignInLauncher),
            signInAsGuest = SignInAsGuestUseCase(authService, sessionRepository),
            sendPasswordReset = SendPasswordResetUseCase(authService),
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region Validación de campos (síncrona, antes de llamar al backend)

    @Test
    fun `given a malformed email when submitting then shows the email format error`() =
        runTest(dispatcher) {
            // Given
            val viewModel = buildViewModel()
            viewModel.onEvent(LoginUiEvent.EmailChanged("not-an-email"))
            viewModel.onEvent(LoginUiEvent.PasswordChanged("secret123"))

            // When
            viewModel.onEvent(LoginUiEvent.Submit)

            // Then
            assertEquals(Res.string.login_error_email_format, viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `given a too-short password when submitting then shows the password length error`() =
        runTest(dispatcher) {
            // Given
            val viewModel = buildViewModel()
            viewModel.onEvent(LoginUiEvent.EmailChanged("ada@lovelace.dev"))
            viewModel.onEvent(LoginUiEvent.PasswordChanged("123"))

            // When
            viewModel.onEvent(LoginUiEvent.Submit)

            // Then
            assertEquals(Res.string.login_error_password_length, viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    // endregion

    // region Login con email

    @Test
    fun `given valid credentials when submitting succeeds then navigates to Main`() =
        runTest(dispatcher) {
            // Given
            val user = AuthUser(uid = "1", email = "ada@lovelace.dev", displayName = "Ada", isGuest = false)
            everySuspend { authService.signInWithEmail("ada@lovelace.dev", "secret123") } returns
                Result.Success(user)
            val viewModel = buildViewModel()
            viewModel.onEvent(LoginUiEvent.EmailChanged("ada@lovelace.dev"))
            viewModel.onEvent(LoginUiEvent.PasswordChanged("secret123"))

            // When
            viewModel.onEvent(LoginUiEvent.Submit)

            // Then
            assertEquals(NavigationEvent.ToMain, viewModel.navigationEvents.first())
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `given invalid credentials when submitting fails then shows an error and stops loading`() =
        runTest(dispatcher) {
            // Given
            everySuspend { authService.signInWithEmail("ada@lovelace.dev", "secret123") } returns
                Result.Failure(AuthError.InvalidCredentials)
            val viewModel = buildViewModel()
            viewModel.onEvent(LoginUiEvent.EmailChanged("ada@lovelace.dev"))
            viewModel.onEvent(LoginUiEvent.PasswordChanged("secret123"))

            // When
            viewModel.onEvent(LoginUiEvent.Submit)
            advanceUntilIdle()

            // Then
            assertFalse(viewModel.uiState.value.isLoading)
            assertTrue(viewModel.uiState.value.errorMessage != null)
        }

    // endregion

    // region Login con Google

    @Test
    fun `given a cancelled Google flow when signing in then stops loading without showing an error`() =
        runTest(dispatcher) {
            // Given: cancelar el diálogo de Google no es un error visible para el usuario
            everySuspend { googleSignInLauncher.requestIdToken() } returns
                Result.Failure(AuthError.GoogleSignInCancelled)
            val viewModel = buildViewModel()

            // When
            viewModel.onEvent(LoginUiEvent.GoogleSignIn)
            advanceUntilIdle()

            // Then
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `given a successful Google flow when signing in then navigates to Main`() =
        runTest(dispatcher) {
            // Given
            val user = AuthUser(uid = "1", email = "ada@lovelace.dev", displayName = "Ada", isGuest = false)
            everySuspend { googleSignInLauncher.requestIdToken() } returns Result.Success("id-token")
            everySuspend { authService.signInWithGoogle("id-token") } returns Result.Success(user)
            val viewModel = buildViewModel()

            // When
            viewModel.onEvent(LoginUiEvent.GoogleSignIn)

            // Then
            assertEquals(NavigationEvent.ToMain, viewModel.navigationEvents.first())
        }

    // endregion

    // region Recuperación de contraseña (SPEC-0002)

    @Test
    fun `given a malformed email when requesting reset then shows the format error in the dialog`() =
        runTest(dispatcher) {
            // Given
            val viewModel = buildViewModel()
            viewModel.onEvent(LoginUiEvent.ForgotEmailChanged("not-an-email"))

            // When
            viewModel.onEvent(LoginUiEvent.SendPasswordReset)

            // Then
            assertEquals(Res.string.login_error_email_format, viewModel.uiState.value.forgotError)
            assertFalse(viewModel.uiState.value.resetConfirmationVisible)
        }

    @Test
    fun `given the reset email is sent when requesting reset then shows the confirmation`() =
        runTest(dispatcher) {
            // Given
            everySuspend { authService.sendPasswordReset("ada@lovelace.dev") } returns Result.Success(Unit)
            val viewModel = buildViewModel()
            viewModel.onEvent(LoginUiEvent.ForgotEmailChanged("ada@lovelace.dev"))

            // When
            viewModel.onEvent(LoginUiEvent.SendPasswordReset)
            advanceUntilIdle()

            // Then
            assertTrue(viewModel.uiState.value.resetConfirmationVisible)
            assertFalse(viewModel.uiState.value.forgotDialogVisible)
            assertNull(viewModel.uiState.value.forgotError)
        }

    @Test
    fun `given an unknown email when requesting reset then still shows the confirmation for anti-enumeration`() =
        runTest(dispatcher) {
            // Given: SPEC-0002 RN-2, un email inexistente se trata como éxito para no filtrar cuentas
            everySuspend { authService.sendPasswordReset("ghost@lovelace.dev") } returns
                Result.Failure(AuthError.UserNotFound)
            val viewModel = buildViewModel()
            viewModel.onEvent(LoginUiEvent.ForgotEmailChanged("ghost@lovelace.dev"))

            // When
            viewModel.onEvent(LoginUiEvent.SendPasswordReset)
            advanceUntilIdle()

            // Then
            assertTrue(viewModel.uiState.value.resetConfirmationVisible)
            assertNull(viewModel.uiState.value.forgotError)
        }

    // endregion
}
