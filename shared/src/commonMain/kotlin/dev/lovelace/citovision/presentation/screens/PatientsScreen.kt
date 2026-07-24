package dev.lovelace.citovision.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import citovision.shared.generated.resources.Res
import citovision.shared.generated.resources.analysis_card_title
import citovision.shared.generated.resources.analysis_code_dialog_hint
import citovision.shared.generated.resources.common_cancel
import citovision.shared.generated.resources.common_close
import citovision.shared.generated.resources.common_retry
import citovision.shared.generated.resources.history_delete_confirm
import citovision.shared.generated.resources.history_delete_title
import citovision.shared.generated.resources.patients_delete_message
import citovision.shared.generated.resources.patients_error_desc
import citovision.shared.generated.resources.patients_error_title
import citovision.shared.generated.resources.patients_id_name_label
import citovision.shared.generated.resources.patients_list_empty
import citovision.shared.generated.resources.patients_list_error
import citovision.shared.generated.resources.patients_list_no_matches
import citovision.shared.generated.resources.patients_list_title
import citovision.shared.generated.resources.patients_new_search
import citovision.shared.generated.resources.patients_no_results_desc
import citovision.shared.generated.resources.patients_no_results_title
import citovision.shared.generated.resources.patients_open_code
import citovision.shared.generated.resources.patients_refresh
import citovision.shared.generated.resources.patients_requires_account_desc
import citovision.shared.generated.resources.patients_requires_account_title
import citovision.shared.generated.resources.patients_result_header
import citovision.shared.generated.resources.patients_search_desc_default
import citovision.shared.generated.resources.patients_search_placeholder
import citovision.shared.generated.resources.patients_search_title
import dev.lovelace.citovision.domain.entities.Analysis
import dev.lovelace.citovision.presentation.components.AnalysisCard
import dev.lovelace.citovision.presentation.components.AnalysisDetailDialog
import dev.lovelace.citovision.presentation.components.dongleIconAlign
import dev.lovelace.citovision.presentation.events.PatientsUiEvent
import dev.lovelace.citovision.presentation.format.formatAnalysisDateTime
import dev.lovelace.citovision.presentation.state.PatientsUiState
import dev.lovelace.citovision.presentation.viewmodels.PatientsViewModel
import dev.lovelace.citovision.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PatientsScreen() {
    val viewModel = koinViewModel<PatientsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // El ViewModel sobrevive al cambio de pestaña, pero esta pantalla no: el efecto se relanza cada vez
    // que se entra en Pacientes, así que el listado refleja los análisis creados mientras tanto.
    LaunchedEffect(Unit) { viewModel.onEvent(PatientsUiEvent.LoadCodes) }
    PatientsContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
private fun PatientsContent(
    uiState: PatientsUiState,
    onEvent: (PatientsUiEvent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, top = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.patients_search_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))

        when {
            uiState.resultsPatientCode != null ->
                ResultsView(code = uiState.resultsPatientCode, results = uiState.results, onEvent = onEvent)

            uiState.isLoading ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            else -> PatientPicker(uiState = uiState, onEvent = onEvent)
        }
    }

    uiState.detail?.let { analysis ->
        AnalysisDetailDialog(
            title = analysis.sampleName ?: stringResource(Res.string.analysis_card_title),
            patient = analysis.patient,
            date = analysis.performedAt.formatAnalysisDateTime(),
            imagePath = analysis.imagePath,
            priority = analysis.priority,
            cellCounts = analysis.cellCounts,
            onDismissRequest = { onEvent(PatientsUiEvent.DismissDetail) },
        )
    }

    uiState.pendingDeletion?.let {
        DeleteConfirmationDialog(
            onConfirm = { onEvent(PatientsUiEvent.ConfirmDelete) },
            onCancel = { onEvent(PatientsUiEvent.CancelDelete) },
        )
    }

    if (uiState.noResultsVisible) {
        InfoDialog(
            title = stringResource(Res.string.patients_no_results_title),
            message = stringResource(Res.string.patients_no_results_desc),
            onDismiss = { onEvent(PatientsUiEvent.DismissNoResults) },
        )
    }
    if (uiState.errorVisible) {
        InfoDialog(
            title = stringResource(Res.string.patients_error_title),
            message = stringResource(Res.string.patients_error_desc),
            onDismiss = { onEvent(PatientsUiEvent.DismissError) },
        )
    }
}

/**
 * Selección de paciente (RF-4b/RF-4c): el campo **filtra** el listado de códigos del usuario y solo se
 * llega a los resultados pulsando uno de ellos, de modo que no se puede consultar un código inexistente.
 */
@Composable
private fun PatientPicker(
    uiState: PatientsUiState,
    onEvent: (PatientsUiEvent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(Res.string.patients_search_desc_default),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.patients_id_name_label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.query,
            onValueChange = { onEvent(PatientsUiEvent.QueryChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(Res.string.patients_search_placeholder)) },
            supportingText = { Text(stringResource(Res.string.analysis_code_dialog_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onEvent(PatientsUiEvent.SubmitQuery) }),
            colors =
                OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedPlaceholderColor = LocalAppColors.current.hint,
                    unfocusedPlaceholderColor = LocalAppColors.current.hint,
                    disabledPlaceholderColor = LocalAppColors.current.hint,
                ),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.patients_list_title),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        PatientCodeList(uiState = uiState, onEvent = onEvent)
    }
}

/** Zona del listado: ocupa el espacio restante y resuelve en ella cargando/error/sin-cuenta/vacío. */
@Composable
private fun ColumnScope.PatientCodeList(
    uiState: PatientsUiState,
    onEvent: (PatientsUiEvent) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .weight(1f),
    ) {
        when {
            uiState.isCodesLoading ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            uiState.requiresAccount ->
                CodesMessage(
                    title = stringResource(Res.string.patients_requires_account_title),
                    message = stringResource(Res.string.patients_requires_account_desc),
                )

            uiState.codesErrorVisible ->
                CodesMessage(
                    title = stringResource(Res.string.patients_error_title),
                    message = stringResource(Res.string.patients_list_error),
                    onRetry = { onEvent(PatientsUiEvent.LoadCodes) },
                )

            uiState.patientCodes.isEmpty() ->
                CodesMessage(message = stringResource(Res.string.patients_list_empty))

            uiState.filteredCodes.isEmpty() ->
                CodesMessage(message = stringResource(Res.string.patients_list_no_matches))

            else ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items = uiState.filteredCodes, key = { it }) { code ->
                        PatientCodeRow(code = code, onClick = { onEvent(PatientsUiEvent.SelectCode(code)) })
                    }
                }
        }
    }
}

/** Fila del listado: toda la fila es pulsable y abre los análisis de ese código. */
@Composable
private fun PatientCodeRow(
    code: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onClick)
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = stringResource(Res.string.patients_open_code, code),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.dongleIconAlign(),
        )
    }
}

/** Estado no-listado de la zona de códigos: sin cuenta, sin pacientes, sin coincidencias o error. */
@Composable
private fun CodesMessage(
    message: String,
    title: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(16.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Text(stringResource(Res.string.common_retry), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ResultsView(
    code: String,
    results: List<Analysis>,
    onEvent: (PatientsUiEvent) -> Unit,
) {
    val title = stringResource(Res.string.analysis_card_title)
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Un código largo no debe comerse los botones: la cabecera cede el ancho sobrante al texto,
            // que se parte en dos líneas como máximo y recorta con puntos suspensivos.
            Text(
                text = stringResource(Res.string.patients_result_header, code),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedIconButton(onClick = { onEvent(PatientsUiEvent.NewSearch) }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(Res.string.patients_new_search),
                    )
                }
                OutlinedIconButton(onClick = { onEvent(PatientsUiEvent.Refresh) }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(Res.string.patients_refresh),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(results) { analysis ->
                AnalysisCard(
                    title = analysis.sampleName ?: title,
                    date = analysis.performedAt.formatAnalysisDateTime(),
                    patient = analysis.patient,
                    description = analysis.summary,
                    imagePath = analysis.imagePath,
                    priority = analysis.priority,
                    onClick = { onEvent(PatientsUiEvent.ShowDetail(analysis)) },
                    onLongClick = { onEvent(PatientsUiEvent.RequestDelete(analysis)) },
                )
            }
        }
    }
}

@Composable
private fun InfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.common_close))
            }
        },
    )
}

@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(Res.string.history_delete_title)) },
        text = { Text(stringResource(Res.string.patients_delete_message)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(Res.string.history_delete_confirm))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel) {
                Text(stringResource(Res.string.common_cancel))
            }
        },
    )
}
