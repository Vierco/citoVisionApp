package dev.lovelace.citovision.presentation.events

/** Acciones de la pantalla de Ajustes. */
sealed interface SettingsUiEvent {
    /** Ir a la pantalla de login para iniciar sesión con una cuenta (p. ej. desde sesión de invitado). */
    data object Login : SettingsUiEvent

    /** Cerrar la sesión actual (cuenta o invitado) y volver al login (SPEC-0001 RF-6). */
    data object SignOut : SettingsUiEvent
}
