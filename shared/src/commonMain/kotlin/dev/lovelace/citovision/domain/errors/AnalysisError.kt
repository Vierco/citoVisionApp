package dev.lovelace.citovision.domain.errors

/**
 * Errores de persistencia de análisis (SPEC-0004). Cerrado: cada caso es un estado conocido.
 * Infrastructure traduce las excepciones de Room y del sistema de ficheros a estos casos.
 */
sealed interface AnalysisError {
    data object NotFound : AnalysisError

    data object StorageFailure : AnalysisError

    data class Unknown(
        val cause: String?,
    ) : AnalysisError
}
