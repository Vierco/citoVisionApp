package dev.lovelace.citovision.infrastructure.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import dev.lovelace.citovision.application.ports.ImageSourceRepository
import dev.lovelace.citovision.domain.settings.ImageSourcePreference
import dev.lovelace.citovision.infrastructure.persistence.preferences.AppPreferenceKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Implementación de [ImageSourceRepository] sobre DataStore Preferences. Guarda el `name` del enum; ante
 * fallo de lectura o valor desconocido devuelve `GALLERY` sin propagar excepción (ver Skill
 * datastore-multiplatform), que es además el comportamiento previo a que existiera la preferencia.
 */
class ImageSourceRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : ImageSourceRepository {
    override fun imageSource(): Flow<ImageSourcePreference> =
        dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { preferences ->
                preferences[AppPreferenceKeys.IMAGE_SOURCE]
                    ?.let { stored -> runCatching { ImageSourcePreference.valueOf(stored) }.getOrNull() }
                    ?: ImageSourcePreference.GALLERY
            }

    override suspend fun setImageSource(preference: ImageSourcePreference) {
        dataStore.edit { preferences -> preferences[AppPreferenceKeys.IMAGE_SOURCE] = preference.name }
    }

    override fun isSourceNoticePending(): Flow<Boolean> =
        dataStore.data
            .map { preferences -> preferences[AppPreferenceKeys.IMAGE_SOURCE_NOTICE_SHOWN] != true }
            // El catch va DESPUÉS del map a propósito: unas preferencias vacías significan "pendiente",
            // así que capturarlas antes haría reaparecer el aviso cada vez que fallara una lectura.
            // Ante el fallo se prefiere no avisar; repetir el aviso molesta más que perderlo.
            .catch { emit(false) }

    override suspend fun markSourceNoticeShown() {
        dataStore.edit { preferences -> preferences[AppPreferenceKeys.IMAGE_SOURCE_NOTICE_SHOWN] = true }
    }
}
