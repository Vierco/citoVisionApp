package dev.lovelace.citovision.presentation.components

/**
 * Convierte una ruta de fichero local en una URI `file://` válida en las cuatro plataformas.
 *
 * En macOS, Linux, Android e iOS la ruta ya empieza por `/`, así que basta con anteponer el esquema y
 * salen las tres barras de rigor (`file:///Users/...`). Windows es el caso distinto: la ruta empieza por
 * la letra de unidad y usa contrabarras (`C:\Users\...`), de modo que concatenar sin más produce
 * `file://C:\Users\...`, que no es una URI válida — con dos barras, lo que sigue se interpreta como
 * *host*, y las contrabarras no son separadores legales. Coil no puede resolverla y la imagen se queda
 * en el placeholder gris. Hay que normalizar a barras y añadir la tercera: `file:///C:/Users/...`.
 *
 * Vive fuera de `Components.kt` para poder cubrirla desde `commonTest`: es lógica pura y determinista,
 * y el defecto que corrige (ADR-0011) solo se manifestaba en una plataforma, que es justo el tipo de
 * regresión que un test detecta y una revisión visual no.
 *
 * **Limitación conocida:** no aplica codificación porcentual, así que un carácter que la URI reserve
 * (un espacio en el nombre de usuario de Windows, por ejemplo) viaja tal cual. No se ha corregido
 * porque no se ha podido reproducir en una máquina real; ver ADR-0011.
 */
internal fun String.toFileUri(): String {
    val normalized = replace('\\', '/')
    return if (normalized.startsWith("/")) "file://$normalized" else "file:///$normalized"
}
