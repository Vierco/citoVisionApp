package dev.lovelace.citovision.application.ports

import dev.lovelace.citovision.domain.settings.ThemePreference
import kotlinx.coroutines.flow.Flow

/**
 * Persistencia de la preferencia de tema (Claro/Oscuro/Seguir sistema). Dato no sensible en DataStore.
 * El acceso a DataStore vive solo en Infrastructure; UI y ViewModel usan este contrato.
 */
interface ThemeRepository {
    fun themePreference(): Flow<ThemePreference>

    suspend fun setThemePreference(preference: ThemePreference)
}
