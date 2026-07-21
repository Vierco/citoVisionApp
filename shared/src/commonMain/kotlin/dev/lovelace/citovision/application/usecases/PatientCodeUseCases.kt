package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.PatientCodeRepository
import kotlinx.coroutines.flow.Flow

/** Observa el último código de paciente usado (cadena vacía si no hay). */
class ObserveLastPatientCodeUseCase(
    private val patientCodeRepository: PatientCodeRepository,
) {
    operator fun invoke(): Flow<String> = patientCodeRepository.lastPatientCode()
}

/** Guarda el código de paciente usado al confirmar el escaneo, para prerrellenar la siguiente muestra. */
class SaveLastPatientCodeUseCase(
    private val patientCodeRepository: PatientCodeRepository,
) {
    suspend operator fun invoke(code: String) = patientCodeRepository.setLastPatientCode(code)
}
