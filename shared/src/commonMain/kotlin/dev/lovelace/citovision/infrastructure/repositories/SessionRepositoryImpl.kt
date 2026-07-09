package dev.lovelace.citovision.infrastructure.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import dev.lovelace.citovision.application.ports.SessionRepository
import dev.lovelace.citovision.infrastructure.persistence.preferences.AppPreferenceKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Implementación de [SessionRepository] sobre DataStore Preferences. Ante fallo de lectura devuelve
 * el valor por defecto (`false`) sin propagar la excepción (ver Skill datastore-multiplatform).
 */
class SessionRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : SessionRepository {
    override fun isGuestSession(): Flow<Boolean> =
        dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { preferences -> preferences[AppPreferenceKeys.GUEST_SESSION] ?: false }

    override suspend fun setGuestSession(active: Boolean) {
        dataStore.edit { preferences -> preferences[AppPreferenceKeys.GUEST_SESSION] = active }
    }
}
