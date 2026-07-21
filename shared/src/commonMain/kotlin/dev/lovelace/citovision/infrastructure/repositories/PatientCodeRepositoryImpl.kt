package dev.lovelace.citovision.infrastructure.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import dev.lovelace.citovision.application.ports.PatientCodeRepository
import dev.lovelace.citovision.infrastructure.persistence.preferences.AppPreferenceKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Implementación de [PatientCodeRepository] sobre DataStore Preferences. Ante fallo de lectura devuelve
 * cadena vacía sin propagar la excepción (ver Skill datastore-multiplatform).
 */
class PatientCodeRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : PatientCodeRepository {
    override fun lastPatientCode(): Flow<String> =
        dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { preferences -> preferences[AppPreferenceKeys.LAST_PATIENT_CODE] ?: "" }

    override suspend fun setLastPatientCode(code: String) {
        dataStore.edit { preferences -> preferences[AppPreferenceKeys.LAST_PATIENT_CODE] = code }
    }

    override suspend fun clear() {
        dataStore.edit { preferences -> preferences.remove(AppPreferenceKeys.LAST_PATIENT_CODE) }
    }
}
