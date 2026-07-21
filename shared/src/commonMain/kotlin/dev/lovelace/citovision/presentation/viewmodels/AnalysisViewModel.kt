package dev.lovelace.citovision.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import citovision.shared.generated.resources.Res
import citovision.shared.generated.resources.analysis_error_generic
import citovision.shared.generated.resources.analysis_error_read_failed
import citovision.shared.generated.resources.analysis_error_save
import citovision.shared.generated.resources.analysis_error_too_large
import citovision.shared.generated.resources.analysis_error_unsupported_format
import dev.lovelace.citovision.application.usecases.AnalysisOutcome
import dev.lovelace.citovision.application.usecases.AnalyzeSampleUseCase
import dev.lovelace.citovision.application.usecases.ObserveLastPatientCodeUseCase
import dev.lovelace.citovision.application.usecases.PickImageUseCase
import dev.lovelace.citovision.application.usecases.ProcessPendingSyncUseCase
import dev.lovelace.citovision.application.usecases.SaveLastPatientCodeUseCase
import dev.lovelace.citovision.application.usecases.SyncAnalysisUseCase
import dev.lovelace.citovision.application.usecases.SyncOutcome
import dev.lovelace.citovision.core.result.fold
import dev.lovelace.citovision.domain.entities.SelectedImage
import dev.lovelace.citovision.domain.errors.ImageError
import dev.lovelace.citovision.domain.validation.isValidPatientCode
import dev.lovelace.citovision.domain.validation.sanitizePatientCode
import dev.lovelace.citovision.presentation.events.AnalysisUiEvent
import dev.lovelace.citovision.presentation.state.AnalysisUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

/**
 * ViewModel de la pestaña Análisis (SPEC-0003 selección de imagen; SPEC-0005 código de paciente + sync;
 * SPEC-0006 inferencia). Procesa la selección de imagen con [PickImageUseCase]; al escanear pide el código,
 * ejecuta el modelo con [analyzeSample] y, según el resultado, guarda y sincroniza, informa de "sin células"
 * o de error de inferencia. Al crearse intenta drenar el outbox pendiente ([processPendingSync]) (RN-8).
 */
class AnalysisViewModel(
    private val pickImage: PickImageUseCase,
    private val analyzeSample: AnalyzeSampleUseCase,
    private val syncAnalysis: SyncAnalysisUseCase,
    private val processPendingSync: ProcessPendingSyncUseCase,
    private val observeLastPatientCode: ObserveLastPatientCodeUseCase,
    private val saveLastPatientCode: SaveLastPatientCodeUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState = _uiState.asStateFlow()

    /** Último código de paciente usado, para prerrellenar el diálogo de la siguiente muestra. */
    private var lastPatientCode: String = ""

    init {
        // Reintento al reabrir (RN-8): best-effort, sin molestar con popup si vuelve a fallar.
        viewModelScope.launch { processPendingSync() }
        observeLastPatientCode().onEach { lastPatientCode = it }.launchIn(viewModelScope)
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
            AnalysisUiEvent.RetryAnalysis -> retryAnalysis()
            AnalysisUiEvent.DismissNoCells -> _uiState.update { it.copy(noCellsVisible = false) }
            AnalysisUiEvent.DismissInferenceError -> _uiState.update { it.copy(inferenceErrorVisible = false) }
            AnalysisUiEvent.RetrySync -> retrySync()
            AnalysisUiEvent.DismissSyncError -> _uiState.update { it.copy(syncErrorVisible = false) }
            AnalysisUiEvent.DismissSavedConfirmation ->
                _uiState.update { it.copy(savedConfirmationVisible = false) }
        }
    }

    private fun openCodeDialog() {
        if (!_uiState.value.canScan) return
        // Prerrelleno con el último código usado (vacío si no hay): se guardan muchas muestras del mismo
        // paciente seguidas. El usuario puede editarlo.
        _uiState.update {
            it.copy(
                codeDialogVisible = true,
                patientCode = lastPatientCode,
                isPatientCodeValid = isValidPatientCode(lastPatientCode),
            )
        }
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
        viewModelScope.launch { saveLastPatientCode(state.patientCode) }
        _uiState.update { it.copy(codeDialogVisible = false) }
        runAnalysis(image, state.patientCode)
    }

    /** Reintenta el análisis con la misma imagen y código tras un fallo de inferencia (RF-7). */
    private fun retryAnalysis() {
        val state = _uiState.value
        val image = state.selectedImage ?: return
        if (state.isSaving) return
        runAnalysis(image, state.patientCode)
    }

    private fun runAnalysis(
        image: SelectedImage,
        patientCode: String,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    error = null,
                    savedConfirmationVisible = false,
                    syncErrorVisible = false,
                    noCellsVisible = false,
                    inferenceErrorVisible = false,
                )
            }
            // La imagen permanece seleccionada para poder analizar otra muestra o reintentar (RF-8).
            when (val outcome = analyzeSample(image, patientCode)) {
                is AnalysisOutcome.Saved -> onAnalysisSaved(outcome.analysisId)
                AnalysisOutcome.NoCellsDetected ->
                    _uiState.update { it.copy(isSaving = false, noCellsVisible = true) }
                AnalysisOutcome.InferenceFailed ->
                    _uiState.update { it.copy(isSaving = false, inferenceErrorVisible = true) }
                AnalysisOutcome.SaveFailed ->
                    _uiState.update { it.copy(isSaving = false, error = Res.string.analysis_error_save) }
            }
        }
    }

    private suspend fun onAnalysisSaved(analysisId: String) {
        val outcome = syncAnalysis(analysisId)
        _uiState.update {
            it.copy(
                isSaving = false,
                savedConfirmationVisible = outcome != SyncOutcome.Failed,
                syncErrorVisible = outcome == SyncOutcome.Failed,
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
