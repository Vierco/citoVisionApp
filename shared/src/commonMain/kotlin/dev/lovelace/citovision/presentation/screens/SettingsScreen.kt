package dev.lovelace.citovision.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale

@Composable
fun SettingsScreen() {
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
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Ajustes",
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
