package dev.lovelace.citovision.presentation.state

import dev.lovelace.citovision.domain.entities.Analysis

/**
 * Estado de la pestaña Pacientes (SPEC-0005). Con [resultsPatientCode] no nulo se muestra la vista de
 * resultados (cabecera + cards); si no, la de selección de paciente (filtro + listado de códigos). Los
 * estados del listado ([isCodesLoading], [requiresAccount], [codesErrorVisible]) son excluyentes entre sí
 * y se resuelven en la propia zona del listado; los popups también son excluyentes.
 */
data class PatientsUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<Analysis> = emptyList(),
    val resultsPatientCode: String? = null,
    val patientCodes: List<String> = emptyList(),
    val isCodesLoading: Boolean = false,
    /**
     * Si el listado ya se resolvió alguna vez en esta sesión. Sirve para enseñar el indicador de carga
     * **solo la primera vez**: a partir de ahí se entra en Pacientes muchas veces (más aún desde que se
     * cambia de pestaña deslizando) y sustituir la lista por un spinner en cada entrada resulta molesto.
     * Las recargas posteriores son silenciosas: se sigue viendo lo que había y se actualiza al llegar.
     */
    val hasLoadedCodes: Boolean = false,
    val codesErrorVisible: Boolean = false,
    val requiresAccount: Boolean = false,
    val detail: Analysis? = null,
    val pendingDeletion: Analysis? = null,
    val noResultsVisible: Boolean = false,
    val errorVisible: Boolean = false,
    /**
     * Ha fallado una **recarga** de los análisis del paciente mostrado. Se distingue de [errorVisible]
     * porque aquella nace de una acción directa (abrir un paciente, borrar) y merece un diálogo, mientras
     * que esta puede ocurrir en segundo plano al entrar en la pestaña: se avisa con un aviso en línea, sin
     * interrumpir ni retirar lo que ya se está viendo.
     */
    val refreshErrorVisible: Boolean = false,
) {
    /** Códigos que sobreviven a la criba de lo escrito en el campo (RF-4b). Sin filtro, todos. */
    val filteredCodes: List<String>
        get() = if (query.isEmpty()) patientCodes else patientCodes.filter { it.contains(query) }
}
