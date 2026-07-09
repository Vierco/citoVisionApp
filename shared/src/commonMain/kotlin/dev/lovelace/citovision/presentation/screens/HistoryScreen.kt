package dev.lovelace.citovision.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import citovision.shared.generated.resources.Res
import citovision.shared.generated.resources.analysis_card_title
import citovision.shared.generated.resources.common_cancel
import citovision.shared.generated.resources.history_delete_confirm
import citovision.shared.generated.resources.history_delete_message
import citovision.shared.generated.resources.history_delete_title
import citovision.shared.generated.resources.history_empty_desc
import citovision.shared.generated.resources.history_empty_title
import dev.lovelace.citovision.presentation.components.AnalysisCard
import dev.lovelace.citovision.presentation.components.AnalysisDetailDialog
import dev.lovelace.citovision.presentation.events.HistoryUiEvent
import dev.lovelace.citovision.presentation.format.formatAnalysisDateTime
import dev.lovelace.citovision.presentation.state.HistoryUiState
import dev.lovelace.citovision.presentation.viewmodels.HistoryViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HistoryScreen() {
    val viewModel = koinViewModel<HistoryViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
private fun HistoryContent(
    uiState: HistoryUiState,
    onEvent: (HistoryUiEvent) -> Unit,
) {
    val analysisTitle = stringResource(Res.string.analysis_card_title)

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

            uiState.isEmpty -> EmptyHistoryContent()

            else ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(uiState.analyses, key = { it.id }) { analysis ->
                        AnalysisCard(
                            title = analysisTitle,
                            date = analysis.performedAt.formatAnalysisDateTime(),
                            patient = analysis.patient,
                            description = analysis.summary,
                            imagePath = analysis.imagePath,
                            onClick = { onEvent(HistoryUiEvent.ShowDetail(analysis)) },
                            onLongClick = { onEvent(HistoryUiEvent.RequestDelete(analysis)) },
                        )
                    }
                }
        }

        uiState.detail?.let { analysis ->
            AnalysisDetailDialog(
                title = analysisTitle,
                patient = analysis.patient,
                date = analysis.performedAt.formatAnalysisDateTime(),
                cellCounts = analysis.cellCounts,
                onDismissRequest = { onEvent(HistoryUiEvent.DismissDetail) },
            )
        }

        if (uiState.pendingDeletion != null) {
            DeleteConfirmationDialog(
                onConfirm = { onEvent(HistoryUiEvent.ConfirmDelete) },
                onDismiss = { onEvent(HistoryUiEvent.CancelDelete) },
            )
        }
    }
}

@Composable
private fun EmptyHistoryContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(horizontal = 64.dp)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(Res.string.history_empty_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(Res.string.history_empty_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Acción destructiva e irreversible: siempre pide confirmación (SPEC-0004 RN-7). */
@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.history_delete_title)) },
        text = { Text(stringResource(Res.string.history_delete_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(Res.string.history_delete_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        },
    )
}
