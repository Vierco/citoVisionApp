package dev.lovelace.citovision.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import citovision.shared.generated.resources.Res
import citovision.shared.generated.resources.analysis_non_diagnostic_notice
import citovision.shared.generated.resources.card_metadata
import citovision.shared.generated.resources.card_view_detail
import citovision.shared.generated.resources.common_close
import citovision.shared.generated.resources.dialog_cell_count_label
import citovision.shared.generated.resources.dialog_cell_label
import citovision.shared.generated.resources.dialog_date_label
import citovision.shared.generated.resources.dialog_low_confidence_entry
import citovision.shared.generated.resources.dialog_low_confidence_label
import citovision.shared.generated.resources.dialog_low_confidence_notice
import citovision.shared.generated.resources.dialog_patient_label
import citovision.shared.generated.resources.dialog_priority_label
import citovision.shared.generated.resources.priority_badge
import citovision.shared.generated.resources.priority_high
import citovision.shared.generated.resources.priority_low
import citovision.shared.generated.resources.priority_medium
import coil3.compose.SubcomposeAsyncImage
import dev.lovelace.citovision.domain.entities.CellCount
import dev.lovelace.citovision.domain.entities.DetectionLevel
import dev.lovelace.citovision.domain.entities.Priority
import dev.lovelace.citovision.ui.theme.error
import dev.lovelace.citovision.ui.theme.getTypography
import dev.lovelace.citovision.ui.theme.secondaryDark
import dev.lovelace.citovision.ui.theme.warning
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

/** Diálogo de detalle de un análisis: paciente, fecha/hora y conteo celular completo (SPEC-0004 RF-4). */
@Composable
fun AnalysisDetailDialog(
    title: String,
    patient: String,
    date: String,
    priority: Priority,
    cellCounts: List<CellCount>,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        // Se libera el ancho por defecto para poder agrandar el diálogo (hay más datos que mostrar).
        modifier =
            Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 640.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
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
                        .heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LabelledValue(stringResource(Res.string.dialog_patient_label), patient)
                LabelledValue(stringResource(Res.string.dialog_date_label), date)

                Column {
                    SectionLabel(stringResource(Res.string.dialog_priority_label))
                    Spacer(modifier = Modifier.height(4.dp))
                    PriorityBadge(priority = priority)
                }

                val standardCounts = cellCounts.filter { it.level == DetectionLevel.STANDARD }
                val reviewCounts = cellCounts.filter { it.level == DetectionLevel.LOW_CONFIDENCE_REVIEW }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionLabel(stringResource(Res.string.dialog_cell_count_label))
                    standardCounts.forEach { cellCount -> CellCountRow(cellCount = cellCount) }
                }

                if (reviewCounts.isNotEmpty()) {
                    LowConfidenceFindings(cellCounts = reviewCounts)
                }

                NonDiagnosticNotice()
            }
        },
        shape = RoundedCornerShape(28.dp),
        // Algo de transparencia en el fondo del diálogo (se intuye el contenido atenuado detrás).
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
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
 * Indicador de la prioridad de revisión (SPEC-0006 RF-3b). No depende solo del color (AGENTS.md): combina
 * un icono direccional (severidad) con la etiqueta textual, usando los tokens semánticos de `DESIGN.md`.
 */
@Composable
fun PriorityBadge(
    priority: Priority,
    modifier: Modifier = Modifier,
) {
    val color: Color
    val icon: ImageVector
    val levelLabel: String
    when (priority) {
        Priority.ALTA -> {
            color = error
            icon = Icons.Filled.KeyboardDoubleArrowUp
            levelLabel = stringResource(Res.string.priority_high)
        }
        Priority.MEDIA -> {
            color = warning
            icon = Icons.Filled.Remove
            levelLabel = stringResource(Res.string.priority_medium)
        }
        Priority.BAJA -> {
            color = secondaryDark
            icon = Icons.Filled.KeyboardDoubleArrowDown
            levelLabel = stringResource(Res.string.priority_low)
        }
    }
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(Res.string.priority_badge, levelLabel),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

/**
 * Fila del conteo celular (SPEC-0006 RF-2). Para tipos celulares reales muestra el recuento y un desplegable
 * con la **confianza del modelo por célula**; las clases no celulares (sin confianzas) se muestran en plano.
 */
@Composable
private fun CellCountRow(cellCount: CellCount) {
    val expandable = cellCount.confidences.isNotEmpty()
    var expanded by remember { mutableStateOf(false) }
    val cellLabel = stringResource(Res.string.dialog_cell_label)
    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(if (expandable) Modifier.clickable { expanded = !expanded } else Modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${cellCount.name}: ${cellCount.count}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            if (expandable) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (expandable && expanded) {
            cellCount.confidences.forEachIndexed { index, confidence ->
                Text(
                    text = "$cellLabel ${index + 1} (conf. ${(confidence * 100).roundToInt()}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
    }
}

/**
 * Posibles hallazgos de baja confianza (SPEC-0006 RF-6b): detecciones de clases críticas en la franja
 * 0,08–0,10 que **no son detecciones confirmadas**. Van en su propia sección, nunca sumadas al conteo, y
 * con la advertencia de revisión humana explícita en texto (no basta el color, AGENTS.md §15).
 */
@Composable
private fun LowConfidenceFindings(cellCounts: List<CellCount>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionLabel(stringResource(Res.string.dialog_low_confidence_label))
        cellCounts.forEach { cellCount ->
            cellCount.confidences.forEach { confidence ->
                Text(
                    text =
                        stringResource(
                            Res.string.dialog_low_confidence_entry,
                            cellCount.name,
                            "${(confidence * 100).roundToInt()}%",
                        ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        Text(
            text = stringResource(Res.string.dialog_low_confidence_notice),
            style = MaterialTheme.typography.bodyMedium,
            color = warning,
        )
    }
}

/** Aviso de no-diagnóstico obligatorio junto al resultado (SPEC-0006 RF-3c, RN-10). */
@Composable
private fun NonDiagnosticNotice() {
    Text(
        text = stringResource(Res.string.analysis_non_diagnostic_notice),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
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
    priority: Priority,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    PriorityBadge(priority = priority)
                }

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
        ImageFallback(modifier = imageModifier)
    } else {
        // Local (SPEC-0004) = ruta de fichero → file://; remoto (SPEC-0005) = URL de descarga de Storage.
        val model = if (imagePath.startsWith("http")) imagePath else "file://$imagePath"
        SubcomposeAsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = imageModifier,
            contentScale = ContentScale.Crop,
            // Mientras la imagen no está en caché (típico en remoto), se muestra un skeleton con shimmer.
            loading = { ShimmerBox(modifier = Modifier.fillMaxSize()) },
            error = { ImageFallback(modifier = Modifier.fillMaxSize()) },
        )
    }
}

/** Placeholder gris para imagen ausente o fallida (RN-5). */
@Composable
private fun ImageFallback(modifier: Modifier) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)))
}

/** Placeholder animado (shimmer) para el hueco de la imagen mientras carga. */
@Composable
private fun ShimmerBox(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "shimmer-progress",
    )
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
    Box(
        modifier =
            modifier.drawBehind {
                val width = size.width
                val start = progress * 2f * width - width
                drawRect(
                    brush =
                        Brush.linearGradient(
                            colors = listOf(base, highlight, base),
                            start = Offset(start, 0f),
                            end = Offset(start + width, 0f),
                        ),
                )
            },
    )
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
            priority = Priority.ALTA,
            onClick = {},
        )
    }
}
