package dev.lovelace.citovision.presentation.state

import dev.lovelace.citovision.domain.entities.SelectedImage
import org.jetbrains.compose.resources.StringResource

/**
 * Estado de la pestaña Análisis (SPEC-0003). El error se expone como recurso ya resuelto por el
 * ViewModel; la UI solo lo muestra. `canScan` habilita el botón "Iniciar Escáner" (RF-5).
 */
data class AnalysisUiState(
    val selectedImage: SelectedImage? = null,
    val isPicking: Boolean = false,
    val isSaving: Boolean = false,
    val savedConfirmationVisible: Boolean = false,
    val error: StringResource? = null,
    val codeDialogVisible: Boolean = false,
    val patientCode: String = "",
    val isPatientCodeValid: Boolean = false,
    val syncErrorVisible: Boolean = false,
    val noCellsVisible: Boolean = false,
    val inferenceErrorVisible: Boolean = false,
) {
    val canScan: Boolean get() = selectedImage != null && !isSaving
}
