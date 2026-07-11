package dev.lovelace.citovision.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lovelace.citovision.application.usecases.DeleteRemoteAnalysisUseCase
import dev.lovelace.citovision.application.usecases.PatientSearchResult
import dev.lovelace.citovision.application.usecases.SearchPatientAnalysesUseCase
import dev.lovelace.citovision.domain.validation.isValidPatientCode
import dev.lovelace.citovision.domain.validation.sanitizePatientCode
import dev.lovelace.citovision.presentation.events.PatientsUiEvent
import dev.lovelace.citovision.presentation.state.PatientsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de la pestaña Pacientes (SPEC-0005). Valida el código (RN-1), consulta remoto por
 * `ownerUid`+código con [search] y traduce el resultado a estados/popups. Solo con cuenta (RF-7).
 */
class PatientsViewModel(
    private val search: SearchPatientAnalysesUseCase,
    private val deleteRemote: DeleteRemoteAnalysisUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PatientsUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: PatientsUiEvent) {
        when (event) {
            is PatientsUiEvent.QueryChanged -> onQueryChanged(event.code)
            PatientsUiEvent.Search -> runSearch()
            PatientsUiEvent.Refresh -> refresh()
            PatientsUiEvent.NewSearch -> _uiState.value = PatientsUiState()
            is PatientsUiEvent.ShowDetail -> _uiState.update { it.copy(detail = event.analysis) }
            PatientsUiEvent.DismissDetail -> _uiState.update { it.copy(detail = null) }
            is PatientsUiEvent.RequestDelete -> _uiState.update { it.copy(pendingDeletion = event.analysis) }
            PatientsUiEvent.ConfirmDelete -> confirmDelete()
            PatientsUiEvent.CancelDelete -> _uiState.update { it.copy(pendingDeletion = null) }
            PatientsUiEvent.DismissNoResults -> _uiState.update { it.copy(noResultsVisible = false) }
            PatientsUiEvent.DismissRequiresAccount -> _uiState.update { it.copy(requiresAccountVisible = false) }
            PatientsUiEvent.DismissError -> _uiState.update { it.copy(errorVisible = false) }
        }
    }

    private fun onQueryChanged(code: String) {
        val sanitized = sanitizePatientCode(code)
        _uiState.update { it.copy(query = sanitized, isQueryValid = isValidPatientCode(sanitized)) }
    }

    private fun runSearch() {
        val state = _uiState.value
        if (state.isLoading || !state.isQueryValid) return
        val code = state.query.trim()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = search(code)) {
                is PatientSearchResult.Found ->
                    _uiState.update {
                        it.copy(isLoading = false, results = result.analyses, resultsPatientCode = code)
                    }

                PatientSearchResult.Empty ->
                    _uiState.update { it.copy(isLoading = false, noResultsVisible = true) }

                PatientSearchResult.RequiresAccount ->
                    _uiState.update { it.copy(isLoading = false, requiresAccountVisible = true) }

                PatientSearchResult.Error ->
                    _uiState.update { it.copy(isLoading = false, errorVisible = true) }
            }
        }
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
                    _uiState.update { it.copy(isLoading = false, requiresAccountVisible = true) }

                PatientSearchResult.Error ->
                    _uiState.update { it.copy(isLoading = false, errorVisible = true) }
            }
        }
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
