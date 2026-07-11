package dev.lovelace.citovision.presentation.events

/** Acciones de la pantalla de Ajustes. */
sealed interface SettingsUiEvent {
    /** Ir a la pantalla de login para iniciar sesión con una cuenta (p. ej. desde sesión de invitado). */
    data object Login : SettingsUiEvent

    /** Cerrar la sesión actual (cuenta o invitado) y volver al login (SPEC-0001 RF-6). */
    data object SignOut : SettingsUiEvent

    /** Borrar todos los análisis locales de la base de datos. */
    data object ClearLocalAnalyses : SettingsUiEvent

    /** Cerrar el aviso de confirmación tras borrar los análisis locales. */
    data object DismissClearedConfirmation : SettingsUiEvent

    /** Abrir el diálogo de envío de feedback. */
    data object OpenFeedback : SettingsUiEvent

    /** El usuario edita el correo de contacto del feedback. */
    data class FeedbackEmailChanged(
        val email: String,
    ) : SettingsUiEvent

    /** El usuario edita el mensaje del feedback. */
    data class FeedbackMessageChanged(
        val message: String,
    ) : SettingsUiEvent

    /** Enviar el feedback a la base de datos remota. */
    data object SubmitFeedback : SettingsUiEvent

    /** Cerrar el diálogo de feedback sin enviar. */
    data object CancelFeedback : SettingsUiEvent

    /** Cerrar el aviso de feedback enviado correctamente. */
    data object DismissFeedbackSent : SettingsUiEvent

    /** Cerrar el aviso de error al enviar el feedback (el diálogo permanece para reintentar). */
    data object DismissFeedbackError : SettingsUiEvent

    /** Volver a la pantalla anterior. */
    data object NavigateBack : SettingsUiEvent
}
