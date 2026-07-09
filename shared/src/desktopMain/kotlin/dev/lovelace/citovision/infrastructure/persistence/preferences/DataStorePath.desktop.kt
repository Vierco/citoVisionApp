package dev.lovelace.citovision.infrastructure.persistence.preferences

import java.io.File

/** Ruta del fichero de preferencias en Desktop: carpeta de datos de la app en el home del usuario. */
fun dataStorePath(): String {
    val directory = File(System.getProperty("user.home"), ".citovision").apply { mkdirs() }
    return File(directory, PREFERENCES_FILE_NAME).absolutePath
}
