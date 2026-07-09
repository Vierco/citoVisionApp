package dev.lovelace.citovision.infrastructure.image

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** Carpeta de imágenes de análisis en iOS: dentro del directorio de documentos de la app. */
@OptIn(ExperimentalForeignApi::class)
fun analysisImagesPath(): String {
    val documentDirectory =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
    return requireNotNull(documentDirectory?.path) + "/$ANALYSIS_IMAGES_DIRECTORY"
}
