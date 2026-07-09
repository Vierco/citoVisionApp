package dev.lovelace.citovision.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import citovision.shared.generated.resources.Res
import citovision.shared.generated.resources.login_error_desktop_unsupported
import citovision.shared.generated.resources.login_error_email_format
import citovision.shared.generated.resources.login_error_generic
import citovision.shared.generated.resources.login_error_invalid_credentials
import citovision.shared.generated.resources.login_error_network
import citovision.shared.generated.resources.login_error_password_length
import citovision.shared.generated.resources.login_error_too_many_requests
import dev.lovelace.citovision.application.usecases.SendPasswordResetUseCase
import dev.lovelace.citovision.application.usecases.SignInAsGuestUseCase
import dev.lovelace.citovision.application.usecases.SignInWithEmailUseCase
import dev.lovelace.citovision.application.usecases.SignInWithGoogleUseCase
import dev.lovelace.citovision.core.result.fold
import dev.lovelace.citovision.domain.errors.AuthError
import dev.lovelace.citovision.presentation.events.LoginUiEvent
import dev.lovelace.citovision.presentation.navigation.NavigationEvent
import dev.lovelace.citovision.presentation.state.LoginUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

class LoginViewModel(
    private val signInWithEmail: SignInWithEmailUseCase,
    private val signInWithGoogle: SignInWithGoogleUseCase,
    private val signInAsGuest: SignInAsGuestUseCase,
    private val sendPasswordReset: SendPasswordResetUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    fun onEvent(event: LoginUiEvent) {
        when (event) {
            is LoginUiEvent.EmailChanged -> _uiState.update { it.copy(email = event.value) }
            is LoginUiEvent.PasswordChanged -> _uiState.update { it.copy(password = event.value) }
            LoginUiEvent.TogglePasswordVisibility ->
                _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
            LoginUiEvent.Submit -> submitEmailLogin()
            LoginUiEvent.GoogleSignIn -> submitGoogleLogin()
            LoginUiEvent.GuestAccess -> submitGuest()
            LoginUiEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }

            LoginUiEvent.OpenForgotPassword ->
                _uiState.update {
                    it.copy(forgotDialogVisible = true, forgotEmail = it.email, forgotError = null)
                }
            LoginUiEvent.DismissForgotPassword ->
                _uiState.update {
                    it.copy(forgotDialogVisible = false, forgotError = null)
                }
            is LoginUiEvent.ForgotEmailChanged -> _uiState.update { it.copy(forgotEmail = event.value) }
            LoginUiEvent.SendPasswordReset -> submitPasswordReset()
            LoginUiEvent.DismissResetConfirmation ->
                _uiState.update { it.copy(resetConfirmationVisible = false) }
        }
    }

    private fun submitEmailLogin() {
        val state = _uiState.value
        if (state.isLoading) return
        if (!EMAIL_REGEX.matches(state.email)) {
            _uiState.update { it.copy(errorMessage = Res.string.login_error_email_format) }
            return
        }
        if (state.password.length < MIN_PASSWORD_LENGTH) {
            _uiState.update { it.copy(errorMessage = Res.string.login_error_password_length) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            signInWithEmail(state.email, state.password).fold(
                onSuccess = { onAuthSuccess() },
                onFailure = { error -> showError(error) },
            )
        }
    }

    private fun submitGoogleLogin() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            signInWithGoogle().fold(
                onSuccess = { onAuthSuccess() },
                onFailure = { error ->
                    if (error == AuthError.GoogleSignInCancelled) {
                        _uiState.update { it.copy(isLoading = false) }
                    } else {
                        showError(error)
                    }
                },
            )
        }
    }

    private fun submitGuest() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            signInAsGuest().fold(
                onSuccess = { onAuthSuccess() },
                onFailure = { error -> showError(error) },
            )
        }
    }

    private fun submitPasswordReset() {
        val state = _uiState.value
        if (state.forgotSending) return
        if (!EMAIL_REGEX.matches(state.forgotEmail)) {
            _uiState.update { it.copy(forgotError = Res.string.login_error_email_format) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(forgotSending = true, forgotError = null) }
            sendPasswordReset(state.forgotEmail).fold(
                onSuccess = { onResetSent() },
                onFailure = { error ->
                    // Anti-enumeración (SPEC-0002 RN-2): un email inexistente se trata como éxito.
                    if (error == AuthError.UserNotFound) {
                        onResetSent()
                    } else {
                        _uiState.update { it.copy(forgotSending = false, forgotError = error.toMessage()) }
                    }
                },
            )
        }
    }

    private suspend fun onAuthSuccess() {
        _uiState.update { it.copy(isLoading = false) }
        _navigationEvents.send(NavigationEvent.ToMain)
    }

    private fun onResetSent() {
        _uiState.update {
            it.copy(
                forgotSending = false,
                forgotDialogVisible = false,
                forgotEmail = "",
                forgotError = null,
                resetConfirmationVisible = true,
            )
        }
    }

    private fun showError(error: AuthError) {
        _uiState.update { it.copy(isLoading = false, errorMessage = error.toMessage()) }
    }

    private fun AuthError.toMessage(): StringResource =
        when (this) {
            AuthError.InvalidCredentials, AuthError.UserNotFound -> Res.string.login_error_invalid_credentials
            AuthError.Network -> Res.string.login_error_network
            AuthError.TooManyRequests -> Res.string.login_error_too_many_requests
            AuthError.NotSupportedOnPlatform -> Res.string.login_error_desktop_unsupported
            AuthError.GoogleSignInCancelled,
            AuthError.GoogleSignInFailed,
            is AuthError.Unknown,
            -> Res.string.login_error_generic
        }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 6
        val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]{2,}$")
    }
}
