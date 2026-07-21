package dev.lovelace.citovision.application.ports

import kotlinx.coroutines.flow.Flow

/**
 * Persistencia del **último código de paciente** usado al guardar una muestra, para prerrellenar el diálogo
 * en la siguiente (SPEC-0005: se guardan muchas muestras seguidas del mismo paciente). Dato no sensible
 * (código seudonimizado, RN-3); se borra al cerrar sesión. El acceso a DataStore vive solo en Infrastructure.
 */
interface PatientCodeRepository {
    fun lastPatientCode(): Flow<String>

    suspend fun setLastPatientCode(code: String)

    suspend fun clear()
}
