package dev.lovelace.citovision.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import citovision.shared.generated.resources.Res
import citovision.shared.generated.resources.app_name
import citovision.shared.generated.resources.common_cancel
import citovision.shared.generated.resources.common_close
import citovision.shared.generated.resources.forgot_dialog_desc
import citovision.shared.generated.resources.forgot_dialog_title
import citovision.shared.generated.resources.forgot_send_button
import citovision.shared.generated.resources.icons_g_144
import citovision.shared.generated.resources.login_button_sign_in
import citovision.shared.generated.resources.login_email_label
import citovision.shared.generated.resources.login_email_placeholder
import citovision.shared.generated.resources.login_error_title
import citovision.shared.generated.resources.login_forgot_password
import citovision.shared.generated.resources.login_google_button
import citovision.shared.generated.resources.login_guest_button
import citovision.shared.generated.resources.login_hide_password
import citovision.shared.generated.resources.login_or_separator
import citovision.shared.generated.resources.login_password_label
import citovision.shared.generated.resources.login_password_placeholder
import citovision.shared.generated.resources.login_secure_access
import citovision.shared.generated.resources.login_show_password
import citovision.shared.generated.resources.reset_sent_desc
import citovision.shared.generated.resources.reset_sent_title
import dev.lovelace.citovision.presentation.components.dongleIconAlign
import dev.lovelace.citovision.presentation.events.LoginUiEvent
import dev.lovelace.citovision.presentation.state.LoginUiState
import dev.lovelace.citovision.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onEvent: (LoginUiEvent) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val backgroundColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .drawBehind {
                    // Fondo base del tema
                    drawRect(backgroundColor)

                    // Efecto de resplandor azul y morado vertical (mezclados en el centro)
                    scale(scaleX = 2.2f, scaleY = 2.5f, pivot = center) {
                        // Resplandor Azul (Posicionado más arriba)
                        val blueCenter = Offset(center.x, center.y - size.height * 0.15f)
                        drawCircle(
                            brush =
                                Brush.radialGradient(
                                    colors =
                                        listOf(
                                            primaryColor.copy(alpha = 0.25f),
                                            Color.Transparent,
                                        ),
                                    center = blueCenter,
                                    radius = size.width * 0.45f,
                                ),
                            radius = size.width * 0.45f,
                            center = blueCenter,
                        )
                        // Resplandor Morado (Tertiary) (Posicionado más abajo)
                        val purpleCenter = Offset(center.x, center.y + size.height * 0.15f)
                        drawCircle(
                            brush =
                                Brush.radialGradient(
                                    colors =
                                        listOf(
                                            tertiaryColor.copy(alpha = 0.20f),
                                            Color.Transparent,
                                        ),
                                    center = purpleCenter,
                                    radius = size.width * 0.4f,
                                ),
                            radius = size.width * 0.4f,
                            center = purpleCenter,
                        )
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Nombre de la app en color terciario
            Text(
                text = stringResource(Res.string.app_name),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = stringResource(Res.string.login_secure_access),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Card con elevación y ligera transparencia
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp), // radius large
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    ),
            ) {
                Column(
                    modifier =
                        Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                ) {
                    // Campo Usuario
                    Text(
                        text = stringResource(Res.string.login_email_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = { onEvent(LoginUiEvent.EmailChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(Res.string.login_email_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions =
                            KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedPlaceholderColor = LocalAppColors.current.hint,
                                unfocusedPlaceholderColor = LocalAppColors.current.hint,
                                disabledPlaceholderColor = LocalAppColors.current.hint,
                            ),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo Contraseña
                    Text(
                        text = stringResource(Res.string.login_password_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = { onEvent(LoginUiEvent.PasswordChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(Res.string.login_password_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            val image =
                                if (uiState.passwordVisible) {
                                    Icons.Default.Visibility
                                } else {
                                    Icons.Default.VisibilityOff
                                }
                            val description =
                                if (uiState.passwordVisible) {
                                    stringResource(Res.string.login_hide_password)
                                } else {
                                    stringResource(Res.string.login_show_password)
                                }
                            IconButton(onClick = { onEvent(LoginUiEvent.TogglePasswordVisibility) }) {
                                Icon(imageVector = image, contentDescription = description)
                            }
                        },
                        visualTransformation =
                            if (uiState.passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions =
                            KeyboardActions(
                                onGo = { if (!uiState.isLoading) onEvent(LoginUiEvent.Submit) },
                            ),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedPlaceholderColor = LocalAppColors.current.hint,
                                unfocusedPlaceholderColor = LocalAppColors.current.hint,
                                disabledPlaceholderColor = LocalAppColors.current.hint,
                            ),
                    )

                    TextButton(
                        onClick = { onEvent(LoginUiEvent.OpenForgotPassword) },
                        modifier = Modifier.align(Alignment.End),
                        enabled = !uiState.isLoading,
                    ) {
                        Text(
                            text = stringResource(Res.string.login_forgot_password),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón Iniciar Sesión (Primary)
                    Button(
                        onClick = { onEvent(LoginUiEvent.Submit) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        shape = RoundedCornerShape(16.dp), // medium
                        enabled = !uiState.isLoading,
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = stringResource(Res.string.login_button_sign_in),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.dongleIconAlign(),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Separador "o"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        )
                        Text(
                            text = stringResource(Res.string.login_or_separator),
                            modifier = Modifier.padding(horizontal = 12.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login con Google
                    OutlinedButton(
                        onClick = { onEvent(LoginUiEvent.GoogleSignIn) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !uiState.isLoading,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.icons_g_144),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp).dongleIconAlign(),
                                tint = Color.Unspecified,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(Res.string.login_google_button),
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer Continuar como invitado
            TextButton(
                onClick = { onEvent(LoginUiEvent.GuestAccess) },
                enabled = !uiState.isLoading,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.login_guest_button),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(">", color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }

    uiState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { onEvent(LoginUiEvent.DismissError) },
            confirmButton = {
                OutlinedButton(onClick = { onEvent(LoginUiEvent.DismissError) }) {
                    Text(stringResource(Res.string.common_close), color = MaterialTheme.colorScheme.primary)
                }
            },
            title = {
                Text(
                    stringResource(Res.string.login_error_title),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Text(
                    stringResource(message),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
        )
    }

    // Diálogo: recuperación de contraseña (SPEC-0002)
    if (uiState.forgotDialogVisible) {
        AlertDialog(
            onDismissRequest = { onEvent(LoginUiEvent.DismissForgotPassword) },
            confirmButton = {
                Button(
                    onClick = { onEvent(LoginUiEvent.SendPasswordReset) },
                    enabled = !uiState.forgotSending,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    if (uiState.forgotSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.forgot_send_button),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onEvent(LoginUiEvent.DismissForgotPassword) },
                    enabled = !uiState.forgotSending,
                ) {
                    Text(stringResource(Res.string.common_cancel), color = MaterialTheme.colorScheme.onSurface)
                }
            },
            title = {
                Text(
                    stringResource(Res.string.forgot_dialog_title),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Column {
                    Text(
                        stringResource(Res.string.forgot_dialog_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.forgotEmail,
                        onValueChange = { onEvent(LoginUiEvent.ForgotEmailChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(Res.string.login_email_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = uiState.forgotError != null,
                        enabled = !uiState.forgotSending,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions =
                            KeyboardActions(
                                onDone = { if (!uiState.forgotSending) onEvent(LoginUiEvent.SendPasswordReset) },
                            ),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedPlaceholderColor = LocalAppColors.current.hint,
                                unfocusedPlaceholderColor = LocalAppColors.current.hint,
                                disabledPlaceholderColor = LocalAppColors.current.hint,
                            ),
                    )
                    uiState.forgotError?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(err),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
        )
    }

    // Diálogo: confirmación genérica tras enviar (anti-enumeración, SPEC-0002 RN-2)
    if (uiState.resetConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { onEvent(LoginUiEvent.DismissResetConfirmation) },
            confirmButton = {
                OutlinedButton(onClick = { onEvent(LoginUiEvent.DismissResetConfirmation) }) {
                    Text(stringResource(Res.string.common_close), color = MaterialTheme.colorScheme.primary)
                }
            },
            title = {
                Text(
                    stringResource(Res.string.reset_sent_title),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Text(
                    stringResource(Res.string.reset_sent_desc),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
        )
    }
}
