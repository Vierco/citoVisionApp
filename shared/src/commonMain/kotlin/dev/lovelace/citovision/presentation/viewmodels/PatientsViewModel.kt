package dev.lovelace.citovision.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lovelace.citovision.application.usecases.DeleteRemoteAnalysisUseCase
import dev.lovelace.citovision.application.usecases.ListPatientCodesUseCase
import dev.lovelace.citovision.application.usecases.PatientCodesResult
import dev.lovelace.citovision.application.usecases.PatientSearchResult
import dev.lovelace.citovision.application.usecases.SearchPatientAnalysesUseCase
import dev.lovelace.citovision.domain.validation.sanitizePatientCode
import dev.lovelace.citovision.presentation.events.PatientsUiEvent
import dev.lovelace.citovision.presentation.state.PatientsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de la pestaña Pacientes (SPEC-0005). Carga con [listPatientCodes] los códigos del usuario
 * para el listado seleccionable (RF-4b), criba ese listado con lo escrito en el campo y, al elegir un
 * código, consulta sus análisis por `ownerUid`+código con [search]. Solo con cuenta (RF-7).
 */
class PatientsViewModel(
    private val search: SearchPatientAnalysesUseCase,
    private val deleteRemote: DeleteRemoteAnalysisUseCase,
    private val listPatientCodes: ListPatientCodesUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PatientsUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: PatientsUiEvent) {
        when (event) {
            is PatientsUiEvent.QueryChanged -> onQueryChanged(event.code)
            PatientsUiEvent.LoadCodes -> loadCodes()
            is PatientsUiEvent.SelectCode -> selectCode(event.code)
            PatientsUiEvent.SubmitQuery -> submitQuery()
            PatientsUiEvent.Refresh -> refresh()
            PatientsUiEvent.NewSearch -> newSearch()
            is PatientsUiEvent.ShowDetail -> _uiState.update { it.copy(detail = event.analysis) }
            PatientsUiEvent.DismissDetail -> _uiState.update { it.copy(detail = null) }
            is PatientsUiEvent.RequestDelete -> _uiState.update { it.copy(pendingDeletion = event.analysis) }
            PatientsUiEvent.ConfirmDelete -> confirmDelete()
            PatientsUiEvent.CancelDelete -> _uiState.update { it.copy(pendingDeletion = null) }
            PatientsUiEvent.DismissNoResults -> _uiState.update { it.copy(noResultsVisible = false) }
            PatientsUiEvent.DismissError -> _uiState.update { it.copy(errorVisible = false) }
        }
    }

    /** El campo es un filtro, no una búsqueda libre: se sanea igual (RN-1) para cribar solo dígitos y guion. */
    private fun onQueryChanged(code: String) {
        _uiState.update { it.copy(query = sanitizePatientCode(code)) }
    }

    private fun loadCodes() {
        if (_uiState.value.isCodesLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCodesLoading = true, codesErrorVisible = false) }
            when (val result = listPatientCodes()) {
                is PatientCodesResult.Loaded ->
                    _uiState.update {
                        it.copy(isCodesLoading = false, patientCodes = result.codes, requiresAccount = false)
                    }

                PatientCodesResult.RequiresAccount ->
                    _uiState.update {
                        it.copy(isCodesLoading = false, patientCodes = emptyList(), requiresAccount = true)
                    }

                PatientCodesResult.Error ->
                    _uiState.update { it.copy(isCodesLoading = false, codesErrorVisible = true) }
            }
        }
    }

    /**
     * Abre los análisis de un código del listado. Como el código procede del propio remoto, el caso
     * "sin resultados" solo puede darse si los análisis se han borrado entre la carga y la selección.
     */
    private fun selectCode(code: String) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, query = code) }
            when (val result = search(code)) {
                is PatientSearchResult.Found ->
                    _uiState.update {
                        it.copy(isLoading = false, results = result.analyses, resultsPatientCode = code)
                    }

                PatientSearchResult.Empty ->
                    _uiState.update { it.copy(isLoading = false, noResultsVisible = true) }

                PatientSearchResult.RequiresAccount ->
                    _uiState.update { it.copy(isLoading = false, requiresAccount = true) }

                PatientSearchResult.Error ->
                    _uiState.update { it.copy(isLoading = false, errorVisible = true) }
            }
        }
    }

    /**
     * Acción "buscar" del teclado: solo lleva a resultados si lo escrito identifica **un** paciente
     * existente (coincidencia exacta o criba con un único superviviente). Nunca busca códigos inventados.
     */
    private fun submitQuery() {
        val state = _uiState.value
        val candidates = state.filteredCodes
        val target = candidates.firstOrNull { it == state.query } ?: candidates.singleOrNull() ?: return
        selectCode(target)
    }

    /** Recarga los análisis del paciente ya mostrado (mismo código) desde el remoto, sin salir de la vista. */
    private fun refresh() {
        val state = _uiState.value
        val code = state.resultsPatientCode ?: return
        if (state.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = search(code)) {
                is PatientSearchResult.Found ->
                    _uiState.update { it.copy(isLoading = false, results = result.analyses) }

                // El paciente ya no tiene análisis (p. ej. se borraron): lista vacía, sin salir de la vista.
                PatientSearchResult.Empty ->
                    _uiState.update { it.copy(isLoading = false, results = emptyList()) }

                PatientSearchResult.RequiresAccount ->
                    _uiState.update { it.copy(isLoading = false, requiresAccount = true) }

                PatientSearchResult.Error ->
                    _uiState.update { it.copy(isLoading = false, errorVisible = true) }
            }
        }
    }

    /** Vuelve al selector limpiando el filtro y recargando el listado (un borrado puede haber vaciado un código). */
    private fun newSearch() {
        _uiState.update { it.copy(query = "", results = emptyList(), resultsPatientCode = null) }
        loadCodes()
    }

    private fun confirmDelete() {
        val target = _uiState.value.pendingDeletion ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(pendingDeletion = null) }
            if (deleteRemote(target.id)) {
                _uiState.update { state -> state.copy(results = state.results.filterNot { it.id == target.id }) }
            } else {
                _uiState.update { it.copy(errorVisible = true) }
            }
        }
    }
}
