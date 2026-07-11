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
import dev.lovelace.citovision.application.usecases.ProcessPendingSyncUseCase
import dev.lovelace.citovision.application.usecases.SaveMockAnalysisUseCase
import dev.lovelace.citovision.application.usecases.SyncAnalysisUseCase
import dev.lovelace.citovision.application.usecases.SyncOutcome
import dev.lovelace.citovision.core.result.fold
import dev.lovelace.citovision.domain.errors.ImageError
import dev.lovelace.citovision.domain.validation.isValidPatientCode
import dev.lovelace.citovision.domain.validation.sanitizePatientCode
import dev.lovelace.citovision.presentation.events.AnalysisUiEvent
import dev.lovelace.citovision.presentation.state.AnalysisUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

/**
 * ViewModel de la pestaña Análisis (SPEC-0003 selección de imagen; SPEC-0005 código de paciente + sync).
 * Procesa la selección de imagen con [PickImageUseCase]; al escanear pide el código, guarda el análisis
 * local ([saveMockAnalysis], andamiaje temporal) y, si hay cuenta, lo sincroniza con [syncAnalysis].
 * Al crearse, intenta drenar el outbox pendiente ([processPendingSync]) de forma silenciosa (RN-8).
 */
class AnalysisViewModel(
    private val pickImage: PickImageUseCase,
    private val saveMockAnalysis: SaveMockAnalysisUseCase,
    private val syncAnalysis: SyncAnalysisUseCase,
    private val processPendingSync: ProcessPendingSyncUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Reintento al reabrir (RN-8): best-effort, sin molestar con popup si vuelve a fallar.
        viewModelScope.launch { processPendingSync() }
    }

    fun onEvent(event: AnalysisUiEvent) {
        when (event) {
            AnalysisUiEvent.SelectImage -> selectImage()
            AnalysisUiEvent.RemoveImage -> _uiState.update { it.copy(selectedImage = null, error = null) }
            AnalysisUiEvent.DismissError -> _uiState.update { it.copy(error = null) }
            AnalysisUiEvent.StartScan -> openCodeDialog()
            is AnalysisUiEvent.PatientCodeChanged -> onCodeChanged(event.code)
            AnalysisUiEvent.ConfirmScan -> confirmScan()
            AnalysisUiEvent.CancelScan -> _uiState.update { it.copy(codeDialogVisible = false) }
            AnalysisUiEvent.RetrySync -> retrySync()
            AnalysisUiEvent.DismissSyncError -> _uiState.update { it.copy(syncErrorVisible = false) }
            AnalysisUiEvent.DismissSavedConfirmation ->
                _uiState.update { it.copy(savedConfirmationVisible = false) }
        }
    }

    private fun openCodeDialog() {
        if (!_uiState.value.canScan) return
        _uiState.update { it.copy(codeDialogVisible = true, patientCode = "", isPatientCodeValid = false) }
    }

    private fun onCodeChanged(code: String) {
        val sanitized = sanitizePatientCode(code)
        _uiState.update {
            it.copy(patientCode = sanitized, isPatientCodeValid = isValidPatientCode(sanitized))
        }
    }

    private fun confirmScan() {
        val state = _uiState.value
        val image = state.selectedImage ?: return
        if (state.isSaving || !state.isPatientCodeValid) return
        val patientCode = state.patientCode
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    codeDialogVisible = false,
                    isSaving = true,
                    error = null,
                    savedConfirmationVisible = false,
                    syncErrorVisible = false,
                )
            }
            saveMockAnalysis(image, patientCode).fold(
                // La imagen permanece seleccionada para poder añadir otro análisis (RF-8).
                onSuccess = { analysisId ->
                    val outcome = syncAnalysis(analysisId)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            savedConfirmationVisible = outcome != SyncOutcome.Failed,
                            syncErrorVisible = outcome == SyncOutcome.Failed,
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isSaving = false, error = Res.string.analysis_error_save) }
                },
            )
        }
    }

    private fun retrySync() {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val synced = processPendingSync()
            _uiState.update {
                it.copy(isSaving = false, syncErrorVisible = !synced, savedConfirmationVisible = synced)
            }
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
