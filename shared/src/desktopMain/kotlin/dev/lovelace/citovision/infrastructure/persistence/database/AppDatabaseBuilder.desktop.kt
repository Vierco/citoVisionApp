package dev.lovelace.citovision.infrastructure.persistence.database

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

/** Builder de la base de datos en Desktop: carpeta de datos de la app en el home del usuario. */
fun appDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val directory = File(System.getProperty("user.home"), ".citovision").apply { mkdirs() }
    return Room.databaseBuilder<AppDatabase>(
        name = File(directory, DATABASE_NAME).absolutePath,
    )
}
