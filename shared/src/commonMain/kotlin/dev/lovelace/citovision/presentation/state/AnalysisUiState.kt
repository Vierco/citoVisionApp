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
    val error: StringResource? = null,
) {
    val canScan: Boolean get() = selectedImage != null
}
