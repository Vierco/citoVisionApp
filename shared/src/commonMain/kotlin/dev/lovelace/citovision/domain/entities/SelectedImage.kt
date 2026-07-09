package dev.lovelace.citovision.domain.entities

/**
 * Imagen de muestra seleccionada por el usuario (SPEC-0003). Se retiene en memoria para su
 * previsualización y para el envío a la API en una fase posterior. No es un tipo de plataforma.
 *
 * No es `data class` a propósito: `equals`/`hashCode` de `ByteArray` son por referencia, así que se
 * implementan a mano con `contentEquals` para tener igualdad estructural real.
 */
class SelectedImage(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SelectedImage) return false
        return fileName == other.fileName &&
            mimeType == other.mimeType &&
            sizeBytes == other.sizeBytes &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + sizeBytes.hashCode()
        return result
    }
}
