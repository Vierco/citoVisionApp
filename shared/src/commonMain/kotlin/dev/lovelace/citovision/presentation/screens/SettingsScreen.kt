package dev.lovelace.citovision.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import citovision.shared.generated.resources.Res
import citovision.shared.generated.resources.nav_settings
import citovision.shared.generated.resources.settings_login
import citovision.shared.generated.resources.settings_logout
import dev.lovelace.citovision.application.usecases.SessionStatus
import dev.lovelace.citovision.presentation.events.SettingsUiEvent
import dev.lovelace.citovision.presentation.state.SettingsUiState
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .drawBehind {
                    // Fondo blanco base
                    drawRect(Color.White)

                    // Efecto de resplandor azul horizontal (rectángulo desenfocado)
                    val primaryColor = Color(0xFF2FA7F0)
                    scale(scaleX = 2.2f, scaleY = 1.6f, pivot = center) {
                        drawCircle(
                            brush =
                                Brush.radialGradient(
                                    colors =
                                        listOf(
                                            primaryColor.copy(alpha = 0.25f),
                                            Color.Transparent,
                                        ),
                                    center = center,
                                    radius = size.width * 0.4f,
                                ),
                            radius = size.width * 0.4f,
                            center = center,
                        )
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.nav_settings),
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(28.dp))

            when (uiState.sessionStatus) {
                SessionStatus.ACCOUNT ->
                    SettingsOption(
                        text = stringResource(Res.string.settings_logout),
                        color = MaterialTheme.colorScheme.error,
                        onClick = { onEvent(SettingsUiEvent.SignOut) },
                    )

                SessionStatus.GUEST ->
                    SettingsOption(
                        text = stringResource(Res.string.settings_login),
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { onEvent(SettingsUiEvent.Login) },
                    )

                SessionStatus.NONE -> Unit
            }
        }
    }
}

@Composable
private fun SettingsOption(
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = color,
        textAlign = TextAlign.Center,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .wrapContentHeight(Alignment.CenterVertically)
                .padding(vertical = 12.dp),
    )
}
