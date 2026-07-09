package dev.lovelace.citovision.infrastructure.persistence.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey

/** Claves de DataStore Preferences centralizadas (ver Skill datastore-multiplatform). */
object AppPreferenceKeys {
    val GUEST_SESSION = booleanPreferencesKey("guest_session")
}
