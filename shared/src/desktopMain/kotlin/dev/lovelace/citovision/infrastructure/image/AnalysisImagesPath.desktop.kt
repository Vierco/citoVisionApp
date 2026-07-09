package dev.lovelace.citovision.infrastructure.image

import java.io.File

/** Carpeta de imágenes de análisis en Desktop: dentro de la carpeta de datos de la app. */
fun analysisImagesPath(): String =
    File(File(System.getProperty("user.home"), ".citovision"), ANALYSIS_IMAGES_DIRECTORY).absolutePath
