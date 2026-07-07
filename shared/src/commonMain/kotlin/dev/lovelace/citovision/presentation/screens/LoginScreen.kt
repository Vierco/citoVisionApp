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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import citovision.shared.generated.resources.Res
import citovision.shared.generated.resources.app_name
import citovision.shared.generated.resources.common_close
import citovision.shared.generated.resources.icons_g_144
import citovision.shared.generated.resources.login_button_sign_in
import citovision.shared.generated.resources.login_email_label
import citovision.shared.generated.resources.login_email_placeholder
import citovision.shared.generated.resources.login_error_email_format
import citovision.shared.generated.resources.login_error_password_chars
import citovision.shared.generated.resources.login_error_password_desc
import citovision.shared.generated.resources.login_forgot_password
import citovision.shared.generated.resources.login_google_button
import citovision.shared.generated.resources.login_guest_button
import citovision.shared.generated.resources.login_hide_password
import citovision.shared.generated.resources.login_or_separator
import citovision.shared.generated.resources.login_password_label
import citovision.shared.generated.resources.login_password_placeholder
import citovision.shared.generated.resources.login_secure_access
import citovision.shared.generated.resources.login_show_password
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    onGuestClick: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var showEmailError by remember { mutableStateOf(false) }
    var showPasswordError by remember { mutableStateOf(false) }

    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]{2,}$".toRegex()
    val passwordRegex = "^[A-Za-z0-9]*$".toRegex()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Fondo blanco base
                drawRect(Color.White)

                // Efecto de resplandor azul horizontal (rectángulo desenfocado)
                val primaryColor = Color(0xFF2FA7F0)
                scale(scaleX = 2.2f, scaleY = 1.6f, pivot = center) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
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
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {

            // Nombre de la app en color terciario
            Text(
                text = stringResource(Res.string.app_name),
                style = MaterialTheme.typography.displayLarge,
                color = Color(0xFFA56AE3), // Tertiary de DESIGN.md
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(Res.string.login_secure_access),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6F6F6F) // onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Card con elevación y ligera transparencia
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp), // radius large
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.85f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    // Campo Usuario
                    Text(
                        text = stringResource(Res.string.login_email_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF282828) // onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(Res.string.login_email_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo Contraseña
                    Text(
                        text = stringResource(Res.string.login_password_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF282828)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            if (passwordRegex.matches(it)) {
                                password = it
                            } else {
                                showPasswordError = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(Res.string.login_password_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            val image = if (passwordVisible)
                                Icons.Default.Visibility
                            else Icons.Default.VisibilityOff

                            val description = if (passwordVisible) stringResource(Res.string.login_hide_password) else stringResource(Res.string.login_show_password)

                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = description)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    TextButton(
                        onClick = { /* TODO */ },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = stringResource(Res.string.login_forgot_password),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF2FA7F0) // primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón Iniciar Sesión (Primary)
                    Button(
                        onClick = {
                            if (emailRegex.matches(email)) {
                                onLoginClick()
                            } else {
                                showEmailError = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2FA7F0) // Primary
                        ),
                        shape = RoundedCornerShape(16.dp) // medium
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(stringResource(Res.string.login_button_sign_in), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Separador "o"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
                        Text(
                            text = stringResource(Res.string.login_or_separator),
                            modifier = Modifier.padding(horizontal = 12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login con Google
                    OutlinedButton(
                        onClick = onGoogleLoginClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.icons_g_144),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(Res.string.login_google_button),
                                color = Color(0xFF282828)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer Continuar como invitado
            TextButton(onClick = onGuestClick) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.login_guest_button),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF282828),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(">", color = Color(0xFF282828))
                }
            }
        }
    }

    if (showEmailError) {
        AlertDialog(
            onDismissRequest = { showEmailError = false },
            confirmButton = {
                OutlinedButton(onClick = { showEmailError = false }) {
                    Text(stringResource(Res.string.common_close), color = Color(0xFF2FA7F0))
                }
            },
            title = {
                Text(
                    stringResource(Res.string.login_error_email_format),
                    color = Color(0xFF282828), // onBackground
                    style = MaterialTheme.typography.titleMedium
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showPasswordError) {
        AlertDialog(
            onDismissRequest = { showPasswordError = false },
            confirmButton = {
                OutlinedButton(onClick = { showPasswordError = false }) {
                    Text(stringResource(Res.string.common_close), color = Color(0xFF2FA7F0))
                }
            },
            title = {
                Text(
                    stringResource(Res.string.login_error_password_chars),
                    color = Color(0xFF282828),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    stringResource(Res.string.login_error_password_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp)
        )
    }
}
