package dev.lovelace.citovision.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import citovision.shared.generated.resources.Res
import citovision.shared.generated.resources.analysis_error_generic
import citovision.shared.generated.resources.analysis_error_read_failed
import citovision.shared.generated.resources.analysis_error_save
import citovision.shared.generated.resources.analysis_error_too_large
import citovision.shared.generated.resources.analysis_error_unsupported_format
import dev.lovelace.citovision.application.usecases.PickImageUseCase
import dev.lovelace.citovision.application.usecases.SaveMockAnalysisUseCase
import dev.lovelace.citovision.core.result.fold
import dev.lovelace.citovision.domain.errors.ImageError
import dev.lovelace.citovision.presentation.events.AnalysisUiEvent
import dev.lovelace.citovision.presentation.state.AnalysisUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

/**
 * ViewModel de la pestaña Análisis (SPEC-0003). Procesa la selección de imagen mediante
 * [PickImageUseCase] y traduce el resultado (imagen, cancelación o error) a [AnalysisUiState].
 *
 * [saveMockAnalysis] es **andamiaje temporal** (SPEC-0004 RF-7): persiste un análisis con datos mock para
 * poder ejercitar el historial sin la IA. Se sustituirá por el caso de uso de análisis real.
 */
class AnalysisViewModel(
    private val pickImage: PickImageUseCase,
    private val saveMockAnalysis: SaveMockAnalysisUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: AnalysisUiEvent) {
        when (event) {
            AnalysisUiEvent.SelectImage -> selectImage()
            AnalysisUiEvent.RemoveImage -> _uiState.update { it.copy(selectedImage = null, error = null) }
            AnalysisUiEvent.DismissError -> _uiState.update { it.copy(error = null) }
            AnalysisUiEvent.StartScan -> startScan()
            AnalysisUiEvent.DismissSavedConfirmation ->
                _uiState.update { it.copy(savedConfirmationVisible = false) }
        }
    }

    /** ⚠️ TEMPORAL: guarda la imagen como fichero y crea la fila mock en la base de datos. */
    private fun startScan() {
        val state = _uiState.value
        val image = state.selectedImage ?: return
        if (state.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, savedConfirmationVisible = false) }
            saveMockAnalysis(image).fold(
                // La imagen permanece seleccionada para poder añadir otro análisis (RF-8).
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, savedConfirmationVisible = true) }
                },
                onFailure = {
                    _uiState.update { it.copy(isSaving = false, error = Res.string.analysis_error_save) }
                },
            )
        }
    }

    private fun selectImage() {
        if (_uiState.value.isPicking) return
        viewModelScope.launch {
            _uiState.update { it.copy(isPicking = true, error = null) }
            pickImage().fold(
                onSuccess = { image ->
                    // image == null → el usuario canceló: se conserva el estado previo (RF-7).
                    _uiState.update { state ->
                        if (image == null) {
                            state.copy(isPicking = false)
                        } else {
                            state.copy(isPicking = false, selectedImage = image, error = null)
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isPicking = false, error = error.toMessage()) }
                },
            )
        }
    }

    private fun ImageError.toMessage(): StringResource =
        when (this) {
            ImageError.UnsupportedFormat -> Res.string.analysis_error_unsupported_format
            ImageError.TooLarge -> Res.string.analysis_error_too_large
            ImageError.ReadFailed -> Res.string.analysis_error_read_failed
            is ImageError.Unknown -> Res.string.analysis_error_generic
        }
}
