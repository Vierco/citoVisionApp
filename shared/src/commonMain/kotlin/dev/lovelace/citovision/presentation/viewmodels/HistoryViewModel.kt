package dev.lovelace.citovision.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import citovision.shared.generated.resources.Res
import citovision.shared.generated.resources.history_error_delete
import dev.lovelace.citovision.application.usecases.DeleteAnalysisUseCase
import dev.lovelace.citovision.application.usecases.ObserveAnalysesUseCase
import dev.lovelace.citovision.core.result.fold
import dev.lovelace.citovision.presentation.events.HistoryUiEvent
import dev.lovelace.citovision.presentation.state.HistoryUiState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de la pestaña Historial (SPEC-0004). Observa los análisis persistidos y gestiona el diálogo
 * de detalle y el borrado con confirmación.
 */
class HistoryViewModel(
    private val observeAnalyses: ObserveAnalysesUseCase,
    private val deleteAnalysis: DeleteAnalysisUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeAnalyses().collect { analyses ->
                // RN-5: un análisis sin imagen es una anomalía en el flujo real.
                if (analyses.any { it.imagePath == null }) {
                    Napier.w("Hay análisis persistidos sin imagen asociada")
                }
                _uiState.update { it.copy(isLoading = false, analyses = analyses) }
            }
        }
    }

    fun onEvent(event: HistoryUiEvent) {
        when (event) {
            is HistoryUiEvent.ShowDetail -> _uiState.update { it.copy(detail = event.analysis) }
            HistoryUiEvent.DismissDetail -> _uiState.update { it.copy(detail = null) }
            is HistoryUiEvent.RequestDelete -> _uiState.update { it.copy(pendingDeletion = event.analysis) }
            HistoryUiEvent.CancelDelete -> _uiState.update { it.copy(pendingDeletion = null) }
            HistoryUiEvent.ConfirmDelete -> confirmDelete()
            HistoryUiEvent.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun confirmDelete() {
        val target = _uiState.value.pendingDeletion ?: return
        _uiState.update { it.copy(pendingDeletion = null) }
        viewModelScope.launch {
            deleteAnalysis(target.id).fold(
                // La lista se refresca sola: el repositorio expone un Flow observable.
                onSuccess = { },
                onFailure = { _uiState.update { state -> state.copy(error = Res.string.history_error_delete) } },
            )
        }
    }
}
