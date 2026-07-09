package dev.lovelace.citovision.domain.errors

/**
 * Errores de selección de imagen de muestra (SPEC-0003). Cerrado: cada caso es un estado conocido.
 * Infrastructure traduce las excepciones técnicas del selector a estos casos.
 */
sealed interface ImageError {
    data object UnsupportedFormat : ImageError

    data object TooLarge : ImageError

    data object ReadFailed : ImageError

    data class Unknown(
        val cause: String?,
    ) : ImageError
}
