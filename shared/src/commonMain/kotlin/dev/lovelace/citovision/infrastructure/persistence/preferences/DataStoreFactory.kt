package dev.lovelace.citovision.infrastructure.persistence.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

/** Nombre del fichero de preferencias, común a todas las plataformas. */
const val PREFERENCES_FILE_NAME = "citovision.preferences_pb"

/**
 * Crea el [DataStore] de Preferences una sola vez (singleton vía Koin). Solo cambia la ruta del
 * fichero según la plataforma, que se resuelve en el módulo de DI correspondiente.
 */
fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })
