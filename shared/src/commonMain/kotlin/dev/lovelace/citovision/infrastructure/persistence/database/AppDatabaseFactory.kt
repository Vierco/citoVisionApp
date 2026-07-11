package dev.lovelace.citovision.infrastructure.persistence.database

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.lovelace.citovision.core.coroutines.ioDispatcher

/** Nombre del fichero de base de datos, común a todas las plataformas. */
const val DATABASE_NAME = "citovision.db"

/**
 * Configuración común de la base de datos: driver SQLite empaquetado (mismo comportamiento en Android,
 * iOS y Desktop) y contexto de corrutinas de E/S. Solo la construcción del `Builder` es de plataforma.
 */
fun createAppDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder
        .addMigrations(MIGRATION_1_2)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(ioDispatcher)
        .build()
