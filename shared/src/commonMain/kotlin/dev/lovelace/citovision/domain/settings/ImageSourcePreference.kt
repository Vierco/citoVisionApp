package dev.lovelace.citovision.domain.settings

/**
 * De dónde se toman las imágenes de muestra (SPEC-0003).
 *
 * En móvil son **dos selectores nativos distintos y ninguno ve el contenido del otro**: `GALLERY` abre
 * el selector de fotos del sistema, que solo lista la fototeca, y `FILES` el explorador de ficheros,
 * que llega a carpetas, descargas y proveedores externos. Por eso la elección es del usuario y vive en
 * Ajustes: la app no puede adivinar dónde guarda cada laboratorio sus muestras.
 *
 * Por defecto `GALLERY`, que es el comportamiento que tenía la app antes de existir esta preferencia.
 *
 * En Desktop la distinción no aplica —solo hay un diálogo de fichero, que lo abarca todo—, así que allí
 * la sección de Ajustes no se muestra (ver `ImagePicker.hasDistinctSources`).
 */
enum class ImageSourcePreference {
    GALLERY,
    FILES,
}
