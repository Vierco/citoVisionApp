package dev.lovelace.citovision.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import citovision.shared.generated.resources.Res
import citovision.shared.generated.resources.analysis_button_scan
import citovision.shared.generated.resources.analysis_change_image
import citovision.shared.generated.resources.analysis_remove_image
import citovision.shared.generated.resources.analysis_scan_hint
import citovision.shared.generated.resources.analysis_select_image
import citovision.shared.generated.resources.analysis_supported_formats
import citovision.shared.generated.resources.analysis_upload_desc
import citovision.shared.generated.resources.analysis_upload_title
import coil3.compose.rememberAsyncImagePainter
import dev.lovelace.citovision.presentation.events.AnalysisUiEvent
import dev.lovelace.citovision.presentation.state.AnalysisUiState
import dev.lovelace.citovision.presentation.viewmodels.AnalysisViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AnalysisScreen() {
    val viewModel = koinViewModel<AnalysisViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AnalysisContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
private fun AnalysisContent(
    uiState: AnalysisUiState,
    onEvent: (AnalysisUiEvent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = stringResource(Res.string.analysis_upload_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.analysis_upload_desc),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Zona de carga: vacía (selector) o con la imagen seleccionada (preview).
        val outlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        if (uiState.selectedImage == null) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(MAX_PREVIEW_HEIGHT)
                        .dashedBorder(outlineColor),
                contentAlignment = Alignment.Center,
            ) {
                EmptyUploadContent(isPicking = uiState.isPicking, onEvent = onEvent)
            }
        } else {
            // El borde punteado se ajusta a la proporción real de la imagen (Fit), acotado en altura.
            // Los bytes en memoria van enteros; el escalado es solo de previsualización.
            val painter = rememberAsyncImagePainter(model = uiState.selectedImage.bytes)
            val intrinsic = painter.intrinsicSize
            val ratio =
                if (intrinsic.isSpecified && intrinsic.height > 0f) {
                    intrinsic.width / intrinsic.height
                } else {
                    null
                }
            Image(
                painter = painter,
                contentDescription = uiState.selectedImage.fileName,
                contentScale = ContentScale.Fit,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(if (ratio != null) Modifier.aspectRatio(ratio) else Modifier.height(MAX_PREVIEW_HEIGHT))
                        .heightIn(max = MAX_PREVIEW_HEIGHT)
                        .dashedBorder(outlineColor)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(20.dp)),
            )
        }

        if (uiState.selectedImage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = uiState.selectedImage.fileName,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { onEvent(AnalysisUiEvent.SelectImage) }, enabled = !uiState.isPicking) {
                    Text(stringResource(Res.string.analysis_change_image))
                }
                TextButton(onClick = { onEvent(AnalysisUiEvent.RemoveImage) }) {
                    Text(
                        text = stringResource(Res.string.analysis_remove_image),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(error),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Botón de escáner: habilitado y en verde secundario solo cuando hay imagen (SPEC-0003 RF-5).
        // La acción real de escanear se implementa en una spec posterior.
        Button(
            onClick = { /* TODO: acción de escáner (spec futura) */ },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            shape = RoundedCornerShape(28.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                    disabledContentColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f),
                ),
            enabled = uiState.canScan,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(Res.string.analysis_button_scan),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.analysis_scan_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun EmptyUploadContent(
    isPicking: Boolean,
    onEvent: (AnalysisUiEvent) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
                Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.FileUpload,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onEvent(AnalysisUiEvent.SelectImage) },
            enabled = !isPicking,
            shape = RoundedCornerShape(28.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            modifier = Modifier.heightIn(min = 56.dp).padding(horizontal = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.analysis_select_image), style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.analysis_supported_formats),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Altura máxima de la zona de preview; también la altura fija del estado vacío. */
private val MAX_PREVIEW_HEIGHT = 280.dp

/** Borde punteado redondeado reutilizable para la zona de carga/preview (SPEC-0003). */
private fun Modifier.dashedBorder(color: Color): Modifier =
    drawBehind {
        drawRoundRect(
            color = color,
            style =
                Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                ),
            cornerRadius = CornerRadius(28.dp.toPx()),
        )
    }
