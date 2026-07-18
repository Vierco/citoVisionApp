package dev.lovelace.citovision.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import citovision.shared.generated.resources.Res
import citovision.shared.generated.resources.app_name
import citovision.shared.generated.resources.splash_initializing
import org.jetbrains.compose.resources.stringResource

@Composable
fun SplashScreen() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .drawBehind {
                    // Fondo blanco base
                    drawRect(Color.White)

                    // Efecto de resplandor azul y morado vertical (mezclados en el centro)
                    val primaryColor = Color(0xFF2FA7F0)
                    val tertiaryColor = Color(0xFFA56AE3)

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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Nombre de la app en color terciario, como en Login pero algo mayor (DESIGN.md).
            Text(
                text = stringResource(Res.string.app_name),
                style =
                    MaterialTheme.typography.displayLarge.copy(
                        fontSize = 88.sp,
                        lineHeight = 80.sp,
                    ),
                color = Color(0xFFA56AE3), // Tertiary de DESIGN.md
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(Res.string.splash_initializing),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}
