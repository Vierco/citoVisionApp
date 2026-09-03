package dev.lovelace.citovision.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import citovision.shared.generated.resources.Res
import citovision.shared.generated.resources.common_accept
import citovision.shared.generated.resources.common_cancel
import citovision.shared.generated.resources.common_close
import citovision.shared.generated.resources.feedback_dialog_desc
import citovision.shared.generated.resources.feedback_dialog_title
import citovision.shared.generated.resources.feedback_error_message
import citovision.shared.generated.resources.feedback_error_title
import citovision.shared.generated.resources.feedback_message_label
import citovision.shared.generated.resources.feedback_message_placeholder
import citovision.shared.generated.resources.feedback_requires_account
import citovision.shared.generated.resources.feedback_send
import citovision.shared.generated.resources.feedback_sent_message
import citovision.shared.generated.resources.feedback_sent_title
import citovision.shared.generated.resources.history_delete_confirm
import citovision.shared.generated.resources.license_body
import citovision.shared.generated.resources.license_univali_body
import citovision.shared.generated.resources.license_univali_title
import citovision.shared.generated.resources.login_email_label
import citovision.shared.generated.resources.login_email_placeholder
import citovision.shared.generated.resources.logout_dialog_desc
import citovision.shared.generated.resources.logout_dialog_title
import citovision.shared.generated.resources.lovelaced
import citovision.shared.generated.resources.settings_clear_analysis
import citovision.shared.generated.resources.settings_clear_confirm_message
import citovision.shared.generated.resources.settings_clear_confirm_title
import citovision.shared.generated.resources.settings_cleared_message
import citovision.shared.generated.resources.settings_cleared_title
import citovision.shared.generated.resources.settings_copyright
import citovision.shared.generated.resources.settings_feedback
import citovision.shared.generated.resources.settings_guest
import citovision.shared.generated.resources.settings_image_source_files
import citovision.shared.generated.resources.settings_image_source_gallery
import citovision.shared.generated.resources.settings_image_source_title
import citovision.shared.generated.resources.settings_license
import citovision.shared.generated.resources.settings_login
import citovision.shared.generated.resources.settings_logout
import citovision.shared.generated.resources.settings_section_actions
import citovision.shared.generated.resources.settings_section_others
import citovision.shared.generated.resources.settings_section_support
import citovision.shared.generated.resources.settings_theme_dark
import citovision.shared.generated.resources.settings_theme_light
import citovision.shared.generated.resources.settings_theme_system
import citovision.shared.generated.resources.settings_theme_title
import citovision.shared.generated.resources.settings_third_party
import citovision.shared.generated.resources.settings_version
import citovision.shared.generated.resources.settings_version_value
import citovision.shared.generated.resources.third_party_androidx
import citovision.shared.generated.resources.third_party_coil
import citovision.shared.generated.resources.third_party_coroutines_datetime
import citovision.shared.generated.resources.third_party_filekit
import citovision.shared.generated.resources.third_party_gitlive
import citovision.shared.generated.resources.third_party_koin
import citovision.shared.generated.resources.third_party_kotlin_compose
import citovision.shared.generated.resources.third_party_ktor
import citovision.shared.generated.resources.third_party_napier
import citovision.shared.generated.resources.third_party_onnx
import citovision.shared.generated.resources.third_party_ultralytics_pending
import citovision.shared.generated.resources.third_party_univali
import citovision.shared.generated.resources.third_party_yolo
import coil3.compose.AsyncImage
import dev.lovelace.citovision.application.usecases.SessionStatus
import dev.lovelace.citovision.domain.settings.ImageSourcePreference
import dev.lovelace.citovision.domain.settings.ThemePreference
import dev.lovelace.citovision.presentation.components.dongleIconAlign
import dev.lovelace.citovision.presentation.events.SettingsUiEvent
import dev.lovelace.citovision.presentation.state.SettingsUiState
import dev.lovelace.citovision.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }
    var showThirdPartyDialog by remember { mutableStateOf(false) }
    var showClearAnalysesDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = stringResource(Res.string.logout_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.logout_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onEvent(SettingsUiEvent.SignOut)
                    },
                    modifier = Modifier.focusRequester(rememberDialogPrimaryFocus()),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(stringResource(Res.string.common_close))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutDialog = false },
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(stringResource(Res.string.common_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
        )
    }

    if (showLicenseDialog) {
        LicenseDialog(onClose = { showLicenseDialog = false })
    }

    if (showThirdPartyDialog) {
        ThirdPartyDialog(
            onOpenUrl = { url -> onEvent(SettingsUiEvent.OpenExternalUrl(url)) },
            onClose = { showThirdPartyDialog = false },
        )
    }

    if (showClearAnalysesDialog) {
        AlertDialog(
            onDismissRequest = { showClearAnalysesDialog = false },
            title = {
                Text(
                    text = stringResource(Res.string.settings_clear_confirm_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.settings_clear_confirm_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearAnalysesDialog = false
                        onEvent(SettingsUiEvent.ClearLocalAnalyses)
                    },
                    modifier = Modifier.focusRequester(rememberDialogPrimaryFocus()),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(stringResource(Res.string.history_delete_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearAnalysesDialog = false },
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(stringResource(Res.string.common_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
        )
    }

    if (uiState.clearedConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { onEvent(SettingsUiEvent.DismissClearedConfirmation) },
            title = {
                Text(
                    text = stringResource(Res.string.settings_cleared_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.settings_cleared_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = { onEvent(SettingsUiEvent.DismissClearedConfirmation) },
                    modifier = Modifier.focusRequester(rememberDialogPrimaryFocus()),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(stringResource(Res.string.common_accept))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
        )
    }

    if (uiState.feedbackDialogVisible) {
        FeedbackDialog(uiState = uiState, onEvent = onEvent)
    }

    if (uiState.feedbackSentVisible) {
        AlertDialog(
            onDismissRequest = { onEvent(SettingsUiEvent.DismissFeedbackSent) },
            title = {
                Text(
                    text = stringResource(Res.string.feedback_sent_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.feedback_sent_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = { onEvent(SettingsUiEvent.DismissFeedbackSent) },
                    modifier = Modifier.focusRequester(rememberDialogPrimaryFocus()),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(stringResource(Res.string.common_accept))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
        )
    }

    if (uiState.feedbackErrorVisible) {
        AlertDialog(
            onDismissRequest = { onEvent(SettingsUiEvent.DismissFeedbackError) },
            title = {
                Text(
                    text = stringResource(Res.string.feedback_error_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.feedback_error_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = { onEvent(SettingsUiEvent.DismissFeedbackError) },
                    modifier = Modifier.focusRequester(rememberDialogPrimaryFocus()),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(stringResource(Res.string.common_accept))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
        )
    }

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
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
            ) {
                IconButton(
                    onClick = { onEvent(SettingsUiEvent.NavigateBack) },
                    modifier = Modifier.align(Alignment.TopStart).offset(x = (-12).dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ProfileAvatar(
                        sessionStatus = uiState.sessionStatus,
                        email = uiState.email,
                        avatarUrl = uiState.avatarUrl,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text =
                            if (uiState.sessionStatus == SessionStatus.ACCOUNT) {
                                uiState.email.orEmpty()
                            } else {
                                stringResource(Res.string.settings_guest)
                            },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Sección Acciones
            SettingsSectionCard(title = stringResource(Res.string.settings_section_actions)) {
                SettingsItem(
                    text = stringResource(Res.string.settings_clear_analysis),
                    icon = Icons.Default.Delete,
                    onClick = { showClearAnalysesDialog = true },
                    color = MaterialTheme.colorScheme.error,
                )

                when (uiState.sessionStatus) {
                    SessionStatus.ACCOUNT -> {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        SettingsItem(
                            text = stringResource(Res.string.settings_logout),
                            icon = Icons.AutoMirrored.Filled.Logout,
                            onClick = { showLogoutDialog = true },
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    SessionStatus.GUEST -> {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        SettingsItem(
                            text = stringResource(Res.string.settings_login),
                            icon = Icons.AutoMirrored.Filled.Login,
                            onClick = { onEvent(SettingsUiEvent.Login) },
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    SessionStatus.NONE -> Unit
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sección Soporte
            SettingsSectionCard(title = stringResource(Res.string.settings_section_support)) {
                SettingsItem(
                    text = stringResource(Res.string.settings_feedback),
                    icon = Icons.Default.Feedback,
                    onClick = { onEvent(SettingsUiEvent.OpenFeedback) },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sección Tema
            SettingsSectionCard(title = stringResource(Res.string.settings_theme_title)) {
                SettingsRadioOption(
                    label = stringResource(Res.string.settings_theme_light),
                    selected = uiState.themePreference == ThemePreference.LIGHT,
                    onClick = { onEvent(SettingsUiEvent.SetTheme(ThemePreference.LIGHT)) },
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                SettingsRadioOption(
                    label = stringResource(Res.string.settings_theme_dark),
                    selected = uiState.themePreference == ThemePreference.DARK,
                    onClick = { onEvent(SettingsUiEvent.SetTheme(ThemePreference.DARK)) },
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                SettingsRadioOption(
                    label = stringResource(Res.string.settings_theme_system),
                    selected = uiState.themePreference == ThemePreference.SYSTEM,
                    onClick = { onEvent(SettingsUiEvent.SetTheme(ThemePreference.SYSTEM)) },
                )
            }

            // Sección Origen de las imágenes: solo donde fototeca y ficheros son selectores distintos
            // (Android e iOS). En Desktop el diálogo de fichero ya llega a todo y no hay nada que elegir.
            if (uiState.imageSourceOptionsVisible) {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionCard(title = stringResource(Res.string.settings_image_source_title)) {
                    SettingsRadioOption(
                        label = stringResource(Res.string.settings_image_source_gallery),
                        selected = uiState.imageSource == ImageSourcePreference.GALLERY,
                        onClick = { onEvent(SettingsUiEvent.SetImageSource(ImageSourcePreference.GALLERY)) },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    SettingsRadioOption(
                        label = stringResource(Res.string.settings_image_source_files),
                        selected = uiState.imageSource == ImageSourcePreference.FILES,
                        onClick = { onEvent(SettingsUiEvent.SetImageSource(ImageSourcePreference.FILES)) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sección Otros
            SettingsSectionCard(title = stringResource(Res.string.settings_section_others)) {
                SettingsItem(
                    text = stringResource(Res.string.settings_license),
                    icon = Icons.Default.Description,
                    onClick = { showLicenseDialog = true },
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                SettingsItem(
                    text = stringResource(Res.string.settings_third_party),
                    icon = Icons.Default.Code,
                    onClick = { showThirdPartyDialog = true },
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                SettingsVersionItem()
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(Res.string.settings_copyright),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalAppColors.current.hint,
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Avatar de la cabecera de Ajustes según el tipo de sesión:
 * - Cuenta con foto (login Google) → muestra su avatar remoto.
 * - Cuenta sin foto (login correo/contraseña) → círculo terciario con la inicial del correo en blanco.
 * - Invitado (o sin sesión) → icono genérico.
 */
@Composable
private fun ProfileAvatar(
    sessionStatus: SessionStatus,
    email: String?,
    avatarUrl: String?,
) {
    val avatarModifier =
        Modifier
            .size(100.dp)
            .clip(CircleShape)
    val initial = email?.trim()?.firstOrNull()?.uppercaseChar()
    when {
        avatarUrl != null ->
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = avatarModifier,
                contentScale = ContentScale.Crop,
            )

        sessionStatus == SessionStatus.ACCOUNT && initial != null ->
            Box(
                modifier = avatarModifier.background(MaterialTheme.colorScheme.tertiary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initial.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiary,
                )
            }

        else ->
            Box(
                modifier = avatarModifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
    }
}

/**
 * Diálogo de envío de feedback: correo de contacto + mensaje, botón primario "Enviar" y "Cancelar"
 * (outlined) a la izquierda. Guarda el feedback en la base de datos remota (MVP, sin correo).
 */
@Composable
private fun FeedbackDialog(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    AlertDialog(
        onDismissRequest = { onEvent(SettingsUiEvent.CancelFeedback) },
        title = {
            Text(
                text = stringResource(Res.string.feedback_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(Res.string.feedback_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                // El feedback se guarda en la base remota, que solo acepta escritura con cuenta: al
                // invitado se le explica por qué "Enviar" está deshabilitado en vez de dejarlo mudo.
                if (uiState.feedbackRequiresAccount) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.feedback_requires_account),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.login_email_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.feedbackEmail,
                    onValueChange = { onEvent(SettingsUiEvent.FeedbackEmailChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(Res.string.login_email_placeholder)) },
                    singleLine = true,
                    enabled = !uiState.feedbackSending,
                    shape = RoundedCornerShape(12.dp),
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
                Text(
                    text = stringResource(Res.string.feedback_message_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.feedbackMessage,
                    onValueChange = { onEvent(SettingsUiEvent.FeedbackMessageChanged(it)) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp),
                    placeholder = { Text(stringResource(Res.string.feedback_message_placeholder)) },
                    enabled = !uiState.feedbackSending,
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedPlaceholderColor = LocalAppColors.current.hint,
                            unfocusedPlaceholderColor = LocalAppColors.current.hint,
                            disabledPlaceholderColor = LocalAppColors.current.hint,
                        ),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onEvent(SettingsUiEvent.SubmitFeedback) },
                enabled = uiState.isFeedbackValid && !uiState.feedbackSending,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (uiState.feedbackSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(Res.string.feedback_send))
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { onEvent(SettingsUiEvent.CancelFeedback) },
                enabled = !uiState.feedbackSending,
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(Res.string.common_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.padding(32.dp).fillMaxSize(),
        properties = DialogProperties(usePlatformDefaultWidth = false),
    )
}

/**
 * Diálogo de licencia: aviso de software propietario y prototipo académico, seguido de la atribución del
 * dataset UNIVALI (CC BY 4.0) y un gráfico provisional. Ocupa la pantalla completa menos 32dp por lado.
 */
@Composable
private fun LicenseDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(
                text = stringResource(Res.string.settings_license),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(Res.string.license_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(Res.string.license_univali_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.license_univali_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Image(
                    painter = painterResource(Res.drawable.lovelaced),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onClose,
                modifier = Modifier.focusRequester(rememberDialogPrimaryFocus()),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(Res.string.common_close))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.padding(32.dp).fillMaxSize(),
        properties = DialogProperties(usePlatformDefaultWidth = false),
    )
}

/**
 * Diálogo de librerías de terceros: cada línea muestra la dependencia con su licencia y, al pulsarla, abre
 * en el navegador la página de esa licencia. Ocupa la pantalla completa menos 32dp por lado.
 */
@Composable
private fun ThirdPartyDialog(
    onOpenUrl: (String) -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(
                text = stringResource(Res.string.settings_third_party),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
            ) {
                thirdPartyLibraries.forEach { (labelRes, url) ->
                    ThirdPartyLink(
                        label = stringResource(labelRes),
                        onClick = { onOpenUrl(url) },
                    )
                    if (labelRes == Res.string.third_party_yolo) {
                        Text(
                            text = stringResource(Res.string.third_party_ultralytics_pending),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalAppColors.current.hint,
                            modifier = Modifier.padding(start = 20.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onClose,
                modifier = Modifier.focusRequester(rememberDialogPrimaryFocus()),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(Res.string.common_close))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.padding(32.dp).fillMaxSize(),
        properties = DialogProperties(usePlatformDefaultWidth = false),
    )
}

/**
 * Una fila de la lista de terceros: viñeta + nombre de la librería en texto normal, donde únicamente el
 * nombre de la licencia (tras el separador «—») es un enlace subrayado que abre su página en el navegador.
 */
@Composable
private fun ThirdPartyLink(
    label: String,
    onClick: () -> Unit,
) {
    val separator = " — "
    val name = label.substringBefore(separator)
    val license = label.substringAfter(separator, "")
    val linkStyles =
        TextLinkStyles(
            style =
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                ),
        )
    val annotated =
        buildAnnotatedString {
            append(name)
            if (license.isNotEmpty()) {
                append(separator)
                withLink(
                    LinkAnnotation.Clickable(
                        tag = license,
                        styles = linkStyles,
                        linkInteractionListener = { onClick() },
                    ),
                ) {
                    append(license)
                }
            }
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = annotated,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Librerías de terceros y la URL de la página de su licencia (SPEC de créditos). El nombre visible procede
 * de recursos; la URL es un dato técnico no localizable, por eso vive aquí y no en `strings.xml`.
 */
private val thirdPartyLibraries: List<Pair<StringResource, String>> =
    listOf(
        Res.string.third_party_yolo to "https://www.gnu.org/licenses/agpl-3.0.html",
        Res.string.third_party_onnx to "https://opensource.org/license/mit",
        Res.string.third_party_univali to "https://creativecommons.org/licenses/by/4.0/",
        Res.string.third_party_kotlin_compose to "https://www.apache.org/licenses/LICENSE-2.0",
        Res.string.third_party_androidx to "https://www.apache.org/licenses/LICENSE-2.0",
        Res.string.third_party_ktor to "https://www.apache.org/licenses/LICENSE-2.0",
        Res.string.third_party_koin to "https://www.apache.org/licenses/LICENSE-2.0",
        Res.string.third_party_coil to "https://www.apache.org/licenses/LICENSE-2.0",
        Res.string.third_party_napier to "https://www.apache.org/licenses/LICENSE-2.0",
        Res.string.third_party_gitlive to "https://www.apache.org/licenses/LICENSE-2.0",
        Res.string.third_party_coroutines_datetime to "https://www.apache.org/licenses/LICENSE-2.0",
        Res.string.third_party_filekit to "https://opensource.org/license/mit",
    )

/**
 * Enfoca el botón primario de un diálogo al abrirse. En escritorio, un botón con foco se activa con Enter
 * por la semántica `clickable` de Compose, así que basta con darle el foco para que Enter equivalga a
 * pulsarlo. Solo se usa en diálogos sin campo de texto, para no robar el foco a la escritura.
 */
@Composable
private fun rememberDialogPrimaryFocus(): FocusRequester {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    return focusRequester
}

/** Una opción de tema (Claro/Oscuro/Seguir sistema): fila seleccionable con radio + etiqueta. */
@Composable
private fun SettingsRadioOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp).dongleIconAlign(),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
        )
    }
}

@Composable
private fun SettingsVersionItem() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp).dongleIconAlign(),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = stringResource(Res.string.settings_version),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(Res.string.settings_version_value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
