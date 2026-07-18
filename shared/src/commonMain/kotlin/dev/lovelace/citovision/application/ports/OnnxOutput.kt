package dev.lovelace.citovision.application.ports

/**
 * Salida cruda de la rama de detección del modelo (SPEC-0006): el tensor [data] aplanado y su número de
 * canales [attributes] (`4 + nº clases + coeficientes de máscara`). El postprocesador deriva de ahí el
 * número de anclas y lee solo caja + clases.
 */
class OnnxOutput(
    val data: FloatArray,
    val attributes: Int,
)
