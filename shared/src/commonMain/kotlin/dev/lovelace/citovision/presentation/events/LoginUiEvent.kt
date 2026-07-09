package dev.lovelace.citovision.presentation.events

/** Acciones de usuario de la pantalla de login, resueltas en LoginViewModel.onEvent(). */
sealed interface LoginUiEvent {
    data class EmailChanged(
        val value: String,
    ) : LoginUiEvent

    data class PasswordChanged(
        val value: String,
    ) : LoginUiEvent

    data object TogglePasswordVisibility : LoginUiEvent

    data object Submit : LoginUiEvent

    data object GoogleSignIn : LoginUiEvent

    data object GuestAccess : LoginUiEvent

    data object DismissError : LoginUiEvent

    // Recuperación de contraseña (SPEC-0002)
    data object OpenForgotPassword : LoginUiEvent

    data object DismissForgotPassword : LoginUiEvent

    data class ForgotEmailChanged(
        val value: String,
    ) : LoginUiEvent

    data object SendPasswordReset : LoginUiEvent

    data object DismissResetConfirmation : LoginUiEvent
}
