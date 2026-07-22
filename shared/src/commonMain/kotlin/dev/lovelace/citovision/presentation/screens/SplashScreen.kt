package dev.lovelace.citovision.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import citovision.shared.generated.resources.Res
import citovision.shared.generated.resources.app_beta
import citovision.shared.generated.resources.app_name
import citovision.shared.generated.resources.splash_initializing
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

@Preview
@Composable
fun SplashScreen() {
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Nombre de la app en color terciario, como en Login pero algo mayor (DESIGN.md). La etiqueta
            // "beta" cuelga del borde derecho del wordmark: el Column con alineación End ajusta su ancho al
            // del wordmark, así que "beta" queda pegada a la derecha de "citoVision".
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(Res.string.app_name),
                    style =
                        MaterialTheme.typography.displayLarge.copy(
                            fontSize = 88.sp,
                            lineHeight = 88.sp,
                        ),
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(Res.string.app_beta),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    // La fuente Dongle declara un descendente enorme bajo "citoVision" que la API común no puede
                    // recortar (no hay includeFontPadding en commonMain y el trim de lineHeight no surte efecto
                    // con esta fuente). Se sube "beta" ese descendente, en sp para que escale también con el
                    // tamaño de fuente del sistema (el lambda de offset resuelve sp a px con densidad y font-scale).
                    modifier = Modifier.offset { IntOffset(x = 0, y = -(57.sp).toPx().roundToInt()) },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(Res.string.splash_initializing),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}
