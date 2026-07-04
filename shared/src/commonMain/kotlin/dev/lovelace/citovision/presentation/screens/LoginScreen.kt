package dev.lovelace.citovision.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    onGuestClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Login",
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Entrar con usuario y contraseña")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onGoogleLoginClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Entrar con Google")
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onGuestClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Modo invitado")
        }
    }
}
