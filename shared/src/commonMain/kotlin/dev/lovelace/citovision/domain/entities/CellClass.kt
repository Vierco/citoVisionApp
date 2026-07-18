package dev.lovelace.citovision.domain.entities

/**
 * Catálogo de las 14 clases del modelo YOLO11s-seg (SPEC-0006, dataset UNIVALI). Fuente única del mapeo
 * **índice → etiqueta ES**, del **peso de relevancia morfológica** (RN-8) y de si la clase es una **célula
 * real** (RN-6). El [index] coincide con el índice de salida del modelo y **no debe alterarse**.
 *
 * `priorityWeight`: puntos que aporta la **presencia** del tipo a la prioridad (RN-8). `isCell`: falso para
 * `Artefacto` y `Restos celulares`, que se muestran con su recuento pero quedan fuera del denominador del
 * porcentaje y no puntúan. `isCritical`: el tipo usa el **umbral de confianza rebajado** de RN-2 para no
 * perder hallazgos débiles; son las cinco clases con métricas de sensibilidad validadas sobre `valid`
 * (`Basófilo` y `Eritroblasto` quedan fuera pese a puntuar en RN-8: con 22 y 44 imágenes en todo el
 * dataset, rebajarles el umbral solo añadiría falsos positivos no medidos).
 */
enum class CellClass(
    val index: Int,
    val label: String,
    val priorityWeight: Int,
    val isCell: Boolean,
    val isCritical: Boolean = false,
) {
    ARTEFACTO(index = 0, label = "Artefacto", priorityWeight = 0, isCell = false),
    BASOFILO(index = 1, label = "Basófilo", priorityWeight = 2, isCell = true),
    BASTONETE(index = 2, label = "Neutrófilo en banda (cayado)", priorityWeight = 1, isCell = true),
    BLASTO(index = 3, label = "Blasto", priorityWeight = 5, isCell = true, isCritical = true),
    EOSINOFILO(index = 4, label = "Eosinófilo", priorityWeight = 0, isCell = true),
    ERITROBLASTO(index = 5, label = "Eritroblasto", priorityWeight = 2, isCell = true),
    LINFOCITO(index = 6, label = "Linfocito", priorityWeight = 0, isCell = true),
    LINFOCITO_ATIPICO(index = 7, label = "Linfocito atípico", priorityWeight = 2, isCell = true, isCritical = true),
    METAMIELOCITO(index = 8, label = "Metamielocito", priorityWeight = 3, isCell = true, isCritical = true),
    MIELOCITO(index = 9, label = "Mielocito", priorityWeight = 3, isCell = true, isCritical = true),
    MONOCITO(index = 10, label = "Monocito", priorityWeight = 0, isCell = true),
    NEUTROFILO_SEGMENTADO(index = 11, label = "Neutrófilo segmentado", priorityWeight = 0, isCell = true),
    PROMIELOCITO(index = 12, label = "Promielocito", priorityWeight = 4, isCell = true, isCritical = true),
    RESTOS_CELULARES(index = 13, label = "Restos celulares", priorityWeight = 0, isCell = false),
    ;

    companion object {
        /** Resuelve la clase por su índice de salida del modelo; `null` si el índice está fuera de rango. */
        fun fromIndex(index: Int): CellClass? = CellClass.entries.firstOrNull { it.index == index }
    }
}
