package dev.lovelace.citovision.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import citovision.shared.generated.resources.Res
import citovision.shared.generated.resources.card_metadata
import citovision.shared.generated.resources.card_view_detail
import citovision.shared.generated.resources.common_close
import citovision.shared.generated.resources.dialog_cell_count_label
import citovision.shared.generated.resources.dialog_date_label
import citovision.shared.generated.resources.dialog_patient_label
import dev.lovelace.citovision.ui.theme.getTypography
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AnalysisDetailDialog(
    title: String,
    patient: String,
    date: String,
    cellCount: String,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            OutlinedButton(
                onClick = onDismissRequest,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF2FA7F0), // Primary
                    ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.common_close),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF282828),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Text(
                        text = stringResource(Res.string.dialog_patient_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6F6F6F),
                    )
                    Text(
                        text = patient,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF282828),
                    )
                }

                Column {
                    Text(
                        text = stringResource(Res.string.dialog_date_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6F6F6F),
                    )
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF282828),
                    )
                }

                Column {
                    Text(
                        text = stringResource(Res.string.dialog_cell_count_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6F6F6F),
                    )
                    Text(
                        text = cellCount,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF282828),
                    )
                }
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White,
    )
}

@Composable
@Preview
fun AnalysisCardPreview() {
    val sampleDescription =
        "Conteo celular completado. Se han detectado neutrófilos y linfocitos según el patrón estándar."
    MaterialTheme(typography = getTypography()) {
        AnalysisCard(
            title = "Análisis de Sangre - Muestra B",
            date = "24/10/2023",
            patient = "PAC-2023-8942",
            description = sampleDescription,
            image = ColorPainter(Color.LightGray),
            onClick = {},
        )
    }
}

@Composable
fun AnalysisCard(
    title: String,
    date: String,
    patient: String,
    description: String,
    image: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp), // medium radius
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp), // high elevation
        colors =
            CardDefaults.cardColors(
                containerColor = Color.White,
            ),
    ) {
        Column {
            // Imagen de la muestra
            Image(
                painter = image,
                contentDescription = title,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                contentScale = ContentScale.Crop,
            )

            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                // Título del análisis
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF282828), // onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Meta información: Fecha y Paciente
                Text(
                    text = stringResource(Res.string.card_metadata, date, patient),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6F6F6F), // onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Descripción o resultados breves
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF282828),
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botón de acción
                Button(
                    onClick = onClick,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2FA7F0), // Primary
                        ),
                    shape = RoundedCornerShape(16.dp), // medium
                ) {
                    Text(
                        text = stringResource(Res.string.card_view_detail),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
