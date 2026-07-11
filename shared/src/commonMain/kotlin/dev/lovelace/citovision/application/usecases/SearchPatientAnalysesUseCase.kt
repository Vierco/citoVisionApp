package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.RemotePatientAnalyses
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.Analysis
import kotlinx.coroutines.flow.first

/**
 * Busca los análisis de un paciente en remoto (SPEC-0005, pestaña Pacientes). Requiere **cuenta iniciada**
 * (RF-7); un invitado obtiene [PatientSearchResult.RequiresAccount]. La consulta se acota al `ownerUid`
 * del usuario (RN-3). Sin resultados → [PatientSearchResult.Empty]; fallo de red → [PatientSearchResult.Error].
 */
class SearchPatientAnalysesUseCase(
    private val authService: AuthService,
    private val remotePatientAnalyses: RemotePatientAnalyses,
) {
    suspend operator fun invoke(patientCode: String): PatientSearchResult {
        val user = authService.currentUser.first()
        if (user == null || user.isGuest) {
            return PatientSearchResult.RequiresAccount
        }
        return when (val result = remotePatientAnalyses.queryByPatient(user.uid, patientCode.trim())) {
            is Result.Success ->
                if (result.value.isEmpty()) {
                    PatientSearchResult.Empty
                } else {
                    PatientSearchResult.Found(result.value)
                }

            is Result.Failure -> PatientSearchResult.Error
        }
    }
}

sealed interface PatientSearchResult {
    data class Found(
        val analyses: List<Analysis>,
    ) : PatientSearchResult

    data object Empty : PatientSearchResult

    data object RequiresAccount : PatientSearchResult

    data object Error : PatientSearchResult
}
