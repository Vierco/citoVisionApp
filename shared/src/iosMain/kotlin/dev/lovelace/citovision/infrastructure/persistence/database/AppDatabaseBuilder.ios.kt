package dev.lovelace.citovision.infrastructure.persistence.database

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** Builder de la base de datos en iOS: directorio de documentos de la app. */
@OptIn(ExperimentalForeignApi::class)
fun appDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val documentDirectory =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
    return Room.databaseBuilder<AppDatabase>(
        name = requireNotNull(documentDirectory?.path) + "/$DATABASE_NAME",
    )
}
