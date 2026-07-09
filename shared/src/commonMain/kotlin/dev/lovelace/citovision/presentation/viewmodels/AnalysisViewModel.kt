package dev.lovelace.citovision.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import citovision.shared.generated.resources.Res
import citovision.shared.generated.resources.analysis_error_generic
import citovision.shared.generated.resources.analysis_error_read_failed
import citovision.shared.generated.resources.analysis_error_too_large
import citovision.shared.generated.resources.analysis_error_unsupported_format
import dev.lovelace.citovision.application.usecases.PickImageUseCase
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
 */
class AnalysisViewModel(
    private val pickImage: PickImageUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: AnalysisUiEvent) {
        when (event) {
            AnalysisUiEvent.SelectImage -> selectImage()
            AnalysisUiEvent.RemoveImage -> _uiState.update { it.copy(selectedImage = null, error = null) }
            AnalysisUiEvent.DismissError -> _uiState.update { it.copy(error = null) }
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
