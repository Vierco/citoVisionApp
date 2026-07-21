package dev.lovelace.citovision.infrastructure.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import dev.lovelace.citovision.application.ports.ThemeRepository
import dev.lovelace.citovision.domain.settings.ThemePreference
import dev.lovelace.citovision.infrastructure.persistence.preferences.AppPreferenceKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Implementación de [ThemeRepository] sobre DataStore Preferences. Guarda el `name` del enum; ante fallo
 * de lectura o valor desconocido devuelve `SYSTEM` sin propagar excepción (ver Skill datastore-multiplatform).
 */
class ThemeRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : ThemeRepository {
    override fun themePreference(): Flow<ThemePreference> =
        dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { preferences ->
                preferences[AppPreferenceKeys.THEME_PREFERENCE]
                    ?.let { stored -> runCatching { ThemePreference.valueOf(stored) }.getOrNull() }
                    ?: ThemePreference.SYSTEM
            }

    override suspend fun setThemePreference(preference: ThemePreference) {
        dataStore.edit { preferences -> preferences[AppPreferenceKeys.THEME_PREFERENCE] = preference.name }
    }
}
