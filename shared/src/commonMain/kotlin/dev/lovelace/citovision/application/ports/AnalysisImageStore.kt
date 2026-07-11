package dev.lovelace.citovision.application.ports

import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.errors.AnalysisError

/**
 * Puerto de almacenamiento de las imágenes de análisis (SPEC-0004 RN-4): la imagen vive como fichero en
 * el almacenamiento privado de la app y en la base de datos solo se guarda su ruta.
 */
interface AnalysisImageStore {
    /** Escribe los bytes en almacenamiento privado y devuelve la ruta del fichero creado. */
    suspend fun save(
        bytes: ByteArray,
        fileName: String,
    ): Result<String, AnalysisError>

    /** Lee los bytes del fichero (para subirlo a remoto, SPEC-0005). Falla si no existe o no se puede leer. */
    suspend fun read(path: String): Result<ByteArray, AnalysisError>

    /** Borra el fichero. Que ya no exista **no** es un error. */
    suspend fun delete(path: String): Result<Unit, AnalysisError>
}
