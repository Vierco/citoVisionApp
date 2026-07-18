package dev.lovelace.citovision.domain.errors

/**
 * Errores del pipeline de inferencia celular on-device (SPEC-0006). Cerrado: cada caso es un estado conocido.
 * Infrastructure traduce las excepciones nativas (decodificado de imagen, ONNX Runtime) a estos casos y el
 * ViewModel los convierte en el popup de error con reintento (RF-7).
 */
sealed interface InferenceError {
    data object ModelLoadFailed : InferenceError

    data object ImageDecodeFailed : InferenceError

    data object InferenceFailed : InferenceError

    data class Unknown(
        val cause: String?,
    ) : InferenceError
}
