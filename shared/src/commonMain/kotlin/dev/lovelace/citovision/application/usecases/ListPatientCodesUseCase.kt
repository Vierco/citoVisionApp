package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.RemotePatientAnalyses
import dev.lovelace.citovision.core.result.Result
import kotlinx.coroutines.flow.first

/**
 * Lista los códigos de paciente del usuario para el selector de la pestaña Pacientes (SPEC-0005 RF-4b).
 * Requiere **cuenta iniciada** (RF-7); un invitado obtiene [PatientCodesResult.RequiresAccount]. Los
 * códigos salen de los análisis remotos del propio `ownerUid` (RN-3); si no hay ninguno, la lista es
 * vacía, que no es un error. Fallo de red → [PatientCodesResult.Error].
 */
class ListPatientCodesUseCase(
    private val authService: AuthService,
    private val remotePatientAnalyses: RemotePatientAnalyses,
) {
    suspend operator fun invoke(): PatientCodesResult {
        val user = authService.currentUser.first()
        if (user == null || user.isGuest) {
            return PatientCodesResult.RequiresAccount
        }
        return when (val result = remotePatientAnalyses.queryPatientCodes(user.uid)) {
            is Result.Success -> PatientCodesResult.Loaded(result.value)
            is Result.Failure -> PatientCodesResult.Error
        }
    }
}

sealed interface PatientCodesResult {
    data class Loaded(
        val codes: List<String>,
    ) : PatientCodesResult

    data object RequiresAccount : PatientCodesResult

    data object Error : PatientCodesResult
}
