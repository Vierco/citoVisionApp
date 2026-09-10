package dev.lovelace.citovision.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
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
import citovision.shared.generated.resources.patients_results_refresh_error
import citovision.shared.generated.resources.patients_search_desc_default
import citovision.shared.generated.resources.patients_search_placeholder
import citovision.shared.generated.resources.patients_search_title
import dev.lovelace.citovision.domain.entities.Analysis
import dev.lovelace.citovision.presentation.components.AnalysisCard
import dev.lovelace.citovision.presentation.components.AnalysisDetailDialog
import dev.lovelace.citovision.presentation.components.ModalOverlayEffect
import dev.lovelace.citovision.presentation.components.dongleIconAlign
import dev.lovelace.citovision.presentation.components.floatingNavigationBarPadding
import dev.lovelace.citovision.presentation.components.rememberSanitizedField
import dev.lovelace.citovision.presentation.events.PatientsUiEvent
import dev.lovelace.citovision.presentation.format.formatAnalysisDateTime
import dev.lovelace.citovision.presentation.state.PatientsUiState
import dev.lovelace.citovision.presentation.viewmodels.PatientsViewModel
import dev.lovelace.citovision.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * [isCurrentTab] es la pestaña en la que está asentado el pager de `MainScreen`. La recarga se ata a él y
 * no al ciclo de vida de esta pantalla, porque dentro de un pager las páginas vecinas se componen
 * **mientras se arrastra**: con un `LaunchedEffect(Unit)` bastaría medio gesto, o uno que se queda a
 * medias, para lanzar consultas al remoto.
 */
@Composable
fun PatientsScreen(isCurrentTab: Boolean) {
    val viewModel = koinViewModel<PatientsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // El ViewModel sobrevive al cambio de pestaña: al volver aquí se recargan el listado de códigos y los
    // análisis del paciente abierto, de modo que aparezca lo escaneado mientras tanto.
    LaunchedEffect(isCurrentTab) {
        if (isCurrentTab) viewModel.onEvent(PatientsUiEvent.Entered)
    }
    PatientsContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
private fun PatientsContent(
    uiState: PatientsUiState,
    onEvent: (PatientsUiEvent) -> Unit,
) {
    // El título ya no vive aquí sino dentro de cada vista, como primer elemento de su lista: así se
    // desplaza con el contenido en vez de robarle sitio fijo. En una pantalla corta —un iPhone SE, o
    // cualquiera con el tipo de letra grande— la cabecera se comía más espacio del que le quedaba a la
    // lista, que llegaba a enseñar una sola fila.
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, top = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            uiState.resultsPatientCode != null ->
                ResultsView(
                    code = uiState.resultsPatientCode,
                    results = uiState.results,
                    refreshError = uiState.refreshErrorVisible,
                    onEvent = onEvent,
                )

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

/** Título de la pantalla. Va dentro de la lista de cada vista para que se desplace con el contenido. */
@Composable
private fun ScreenTitle() {
    Text(
        text = stringResource(Res.string.patients_search_title),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Selección de paciente (RF-4b/RF-4c): el campo **filtra** el listado de códigos del usuario y solo se
 * llega a los resultados pulsando uno de ellos, de modo que no se puede consultar un código inexistente.
 *
 * Toda la vista es **una sola lista desplazable**: título, descripción y campo son sus primeros elementos
 * y los códigos van detrás. Así la cabecera se aparta al desplazar y el listado dispone de la pantalla
 * entera, en vez de repartirse lo que sobre de una cabecera fija.
 */
@Composable
private fun PatientPicker(
    uiState: PatientsUiState,
    onEvent: (PatientsUiEvent) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp + floatingNavigationBarPadding()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            PickerHeader(query = uiState.query, focusManager = focusManager, onEvent = onEvent)
        }
        patientCodeItems(uiState = uiState, onEvent = onEvent)
    }
}

/** Cabecera del selector: título, explicación y campo de filtro. Es el primer elemento de la lista. */
@Composable
private fun PickerHeader(
    query: String,
    focusManager: FocusManager,
    onEvent: (PatientsUiEvent) -> Unit,
) {
    // Mismo motivo que en el diálogo del escáner: el texto vuelve saneado y la selección se lleva aquí
    // para que el cursor no salte (ver `rememberSanitizedField`).
    val field = rememberSanitizedField(text = query, onTextChange = { onEvent(PatientsUiEvent.QueryChanged(it)) })
    Column {
        ScreenTitle()
        Spacer(modifier = Modifier.height(16.dp))
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
            value = field.value,
            onValueChange = field.onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(Res.string.patients_search_placeholder)) },
            supportingText = { Text(stringResource(Res.string.analysis_code_dialog_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            // Cerrar el teclado tiene que ser explícito: Compose solo lo oculta por defecto con
            // `ImeAction.Done`; para `Search` no hace nada (ver `KeyboardActionRunner`). Antes solo
            // desaparecía de rebote, cuando `SubmitQuery` resolvía un paciente y este campo salía de la
            // composición. Si el filtro no dejaba un único código, no pasaba nada y no había salida.
            keyboardActions =
                KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        onEvent(PatientsUiEvent.SubmitQuery)
                    },
                ),
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
    }
}

/**
 * Aviso en línea para una recarga que ha fallado **cuando lo que ya hay en pantalla se sigue pudiendo
 * usar**. No interrumpe como haría un diálogo, porque estas recargas se lanzan solas al entrar en la
 * pestaña y el usuario no ha pedido nada: informa en dos líneas como mucho y ofrece reintentar.
 *
 * Solo se usa en la vista de un paciente, y la distinción importa: sus cards **se leen sin red** (resumen,
 * prioridad, fecha, y el diálogo de detalle sale de datos ya cargados), así que conservarlas es útil. El
 * listado de códigos es el caso contrario —cada elemento es una consulta al remoto— y allí sí se retira,
 * porque ofrecer una lista que no abre sería prometer algo que no se puede cumplir.
 *
 * Los errores nacidos de una acción directa (abrir un paciente, borrar) siguen siendo diálogo.
 */
@Composable
private fun InlineErrorBanner(
    message: String,
    onRetry: () -> Unit,
) {
    val errorColor = MaterialTheme.colorScheme.error
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(errorColor.copy(alpha = 0.15f))
                .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) {
            Text(
                text = stringResource(Res.string.common_retry),
                style = MaterialTheme.typography.labelLarge,
                color = errorColor,
            )
        }
    }
}

/**
 * Elementos del listado, que van detrás de la cabecera **dentro de la misma lista**. Resuelve aquí sus
 * estados: cargando, sin cuenta, error, vacío, sin coincidencias, o los códigos.
 *
 * Al ser elementos y no una zona de altura fija, los mensajes se desplazan con el resto: antes, en una
 * pantalla corta, el botón de reintentar quedaba fuera y no había forma de llegar a él.
 */
private fun LazyListScope.patientCodeItems(
    uiState: PatientsUiState,
    onEvent: (PatientsUiEvent) -> Unit,
) {
    when {
        // El indicador solo la primera vez. Después se entra en Pacientes constantemente, y sustituir
        // la lista por un spinner en cada entrada es peor que no avisar: la recarga es silenciosa y el
        // listado se actualiza al llegar, sin parpadeo, porque la `LazyColumn` no llega a desmontarse.
        uiState.isCodesLoading && !uiState.hasLoadedCodes ->
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

        uiState.requiresAccount ->
            item {
                CodesMessage(
                    title = stringResource(Res.string.patients_requires_account_title),
                    message = stringResource(Res.string.patients_requires_account_desc),
                )
            }

        // El error sustituye al listado aunque ya hubiera códigos cargados, y es a propósito: esta
        // pestaña es enteramente remota (SPEC-0005), así que sin conexión cada código de la lista es
        // una puerta que no abre — tocarlo solo llevaría al diálogo de error de `selectCode`. Mostrar
        // una lista intocable sería prometer algo que no se puede cumplir.
        uiState.codesErrorVisible ->
            item {
                CodesMessage(
                    title = stringResource(Res.string.patients_error_title),
                    message = stringResource(Res.string.patients_list_error),
                    onRetry = { onEvent(PatientsUiEvent.LoadCodes) },
                )
            }

        uiState.patientCodes.isEmpty() ->
            item { CodesMessage(message = stringResource(Res.string.patients_list_empty)) }

        uiState.filteredCodes.isEmpty() ->
            item { CodesMessage(message = stringResource(Res.string.patients_list_no_matches)) }

        else ->
            items(items = uiState.filteredCodes, key = { it }) { code ->
                PatientCodeRow(code = code, onClick = { onEvent(PatientsUiEvent.SelectCode(code)) })
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
    // Ancho completo y alto según su contenido: vive dentro de una lista, donde el alto es ilimitado y
    // un `fillMaxSize` no tendría contra qué medirse.
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
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
    refreshError: Boolean,
    onEvent: (PatientsUiEvent) -> Unit,
) {
    val title = stringResource(Res.string.analysis_card_title)
    // Misma estructura que el selector: una sola lista, con el título y la cabecera como primer elemento
    // para que se aparten al desplazar en vez de ocupar sitio fijo en una pantalla corta.
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp + floatingNavigationBarPadding()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                ScreenTitle()
                Spacer(modifier = Modifier.height(16.dp))
                ResultsHeader(code = code, onEvent = onEvent)
            }
        }

        // Si la recarga de las muestras falla, se avisa sin quitar lo que hay: estas cards se leen sin red.
        if (refreshError) {
            item {
                InlineErrorBanner(
                    message = stringResource(Res.string.patients_results_refresh_error),
                    onRetry = { onEvent(PatientsUiEvent.Refresh) },
                )
            }
        }

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

/** Cabecera de la vista de resultados: a qué paciente pertenecen y las acciones sobre ellos. */
@Composable
private fun ResultsHeader(
    code: String,
    onEvent: (PatientsUiEvent) -> Unit,
) {
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
}

@Composable
private fun InfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
) {
    ModalOverlayEffect()
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
    ModalOverlayEffect()
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
