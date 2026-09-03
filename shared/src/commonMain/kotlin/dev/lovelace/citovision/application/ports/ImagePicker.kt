package dev.lovelace.citovision.application.ports

import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.SelectedImage
import dev.lovelace.citovision.domain.errors.ImageError
import dev.lovelace.citovision.domain.settings.ImageSourcePreference

/**
 * Puerto que abre el selector de imágenes nativo de la plataforma (SPEC-0003, RNF-1). La obtención de
 * la imagen es específica de plataforma (Android e iOS: fototeca o explorador de ficheros; Desktop:
 * diálogo de fichero), por eso vive tras esta abstracción. La validación de formato/tamaño la aplica el
 * use case, no el puerto.
 */
interface ImagePicker {
    /**
     * `true` donde fototeca y ficheros son **dos selectores distintos** que no ven el contenido del
     * otro (Android e iOS). En Desktop es `false`: un único diálogo de fichero lo abarca todo, así que
     * ni se ofrece la preferencia en Ajustes ni tiene sentido el aviso.
     */
    val hasDistinctSources: Boolean

    /**
     * `true` si el selector puede abrirse **en la misma acción** que cierra un diálogo.
     *
     * En iOS es `false`. Los diálogos de Compose viven allí en una `UIWindow` aparte a nivel alerta, y
     * el selector se presenta sobre la *key window*: pedirlo mientras esa ventana se desmonta lo deja
     * presentado sobre algo que desaparece, sin abrirse y sin reanudar nunca su corrutina. Donde vale
     * `false`, la UI tiene que pedir al usuario que vuelva a pulsar.
     */
    val canOpenPickerAfterDialog: Boolean

    /** Abre el selector. Devuelve `null` si el usuario cancela (SPEC-0003 RF-7). */
    suspend fun pickImage(source: ImageSourcePreference): Result<SelectedImage?, ImageError>
}
