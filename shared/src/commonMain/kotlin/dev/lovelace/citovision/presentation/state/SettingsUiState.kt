package dev.lovelace.citovision.presentation.state

import dev.lovelace.citovision.application.usecases.SessionStatus

/**
 * Estado de la pantalla de Ajustes. El tipo de sesión decide qué opción se muestra:
 * cuenta → cerrar sesión; invitado → iniciar sesión con cuenta. `NONE` no muestra ninguna.
 */
data class SettingsUiState(
    val sessionStatus: SessionStatus = SessionStatus.NONE,
)
