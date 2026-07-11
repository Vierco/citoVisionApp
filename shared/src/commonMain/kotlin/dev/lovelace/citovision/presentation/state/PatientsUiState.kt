package dev.lovelace.citovision.presentation.state

import dev.lovelace.citovision.domain.entities.Analysis

/**
 * Estado de la pestaña Pacientes (SPEC-0005). Con [resultsPatientCode] no nulo se muestra la vista de
 * resultados (cabecera + cards); si no, la de entrada de código. Los popups son excluyentes.
 */
data class PatientsUiState(
    val query: String = "",
    val isQueryValid: Boolean = false,
    val isLoading: Boolean = false,
    val results: List<Analysis> = emptyList(),
    val resultsPatientCode: String? = null,
    val detail: Analysis? = null,
    val pendingDeletion: Analysis? = null,
    val noResultsVisible: Boolean = false,
    val requiresAccountVisible: Boolean = false,
    val errorVisible: Boolean = false,
)
