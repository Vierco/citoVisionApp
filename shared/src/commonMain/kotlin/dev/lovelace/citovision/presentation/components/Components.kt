package dev.lovelace.citovision.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import coil3.compose.AsyncImage
import dev.lovelace.citovision.domain.entities.CellCount
import dev.lovelace.citovision.ui.theme.getTypography
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Diálogo de detalle de un análisis: paciente, fecha/hora y conteo celular completo (SPEC-0004 RF-4). */
@Composable
fun AnalysisDetailDialog(
    title: String,
    patient: String,
    date: String,
    cellCounts: List<CellCount>,
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
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
            ) {
                Text(
                    text = stringResource(Res.string.common_close),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        },
        text = {
            // El conteo celular tiene longitud variable: el contenido debe poder desplazarse.
            Column(
                modifier =
                    Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LabelledValue(stringResource(Res.string.dialog_patient_label), patient)
                LabelledValue(stringResource(Res.string.dialog_date_label), date)

                Column {
                    SectionLabel(stringResource(Res.string.dialog_cell_count_label))
                    cellCounts.forEach { cellCount ->
                        Text(
                            text = "${cellCount.name}: ${cellCount.value}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun LabelledValue(
    label: String,
    value: String,
) {
    Column {
        SectionLabel(label)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/**
 * Card de un análisis del historial. [onLongClick] ofrece el borrado (SPEC-0004 RF-5).
 * Si [imagePath] es nulo o el fichero no existe, se muestra un placeholder gris (RN-5).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnalysisCard(
    title: String,
    date: String,
    patient: String,
    description: String,
    imagePath: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            AnalysisCardImage(imagePath = imagePath, contentDescription = title)

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(Res.string.card_metadata, date, patient),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onClick,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.card_view_detail),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalysisCardImage(
    imagePath: String?,
    contentDescription: String,
) {
    val imageModifier =
        Modifier
            .fillMaxWidth()
            .height(180.dp)

    if (imagePath == null) {
        Box(modifier = imageModifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)))
    } else {
        AsyncImage(
            model = "file://$imagePath",
            contentDescription = contentDescription,
            modifier = imageModifier,
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
@Preview
fun AnalysisCardPreview() {
    val sampleDescription =
        "Conteo celular completado. Se han detectado neutrófilos y linfocitos según el patrón estándar."
    MaterialTheme(typography = getTypography()) {
        AnalysisCard(
            title = "Análisis",
            date = "24/10/2023 18:43",
            patient = "PAC-2023-8942",
            description = sampleDescription,
            imagePath = null,
            onClick = {},
        )
    }
}
