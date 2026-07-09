package dev.lovelace.citovision.application.ports

import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.SelectedImage
import dev.lovelace.citovision.domain.errors.ImageError

/**
 * Puerto que abre el selector de imágenes nativo de la plataforma (SPEC-0003, RNF-1). La obtención de
 * la imagen es específica de plataforma (Android: selector del sistema; Desktop: diálogo de fichero),
 * por eso vive tras esta abstracción. La validación de formato/tamaño la aplica el use case, no el puerto.
 */
interface ImagePicker {
    /** Abre el selector. Devuelve `null` si el usuario cancela (SPEC-0003 RF-7). */
    suspend fun pickImage(): Result<SelectedImage?, ImageError>
}
