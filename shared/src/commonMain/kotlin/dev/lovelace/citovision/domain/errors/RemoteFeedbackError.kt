package dev.lovelace.citovision.domain.errors

/**
 * Errores del envío remoto de feedback (Firestore por REST). Cerrado. Infrastructure traduce las
 * excepciones de red y de serialización a estos casos.
 */
sealed interface RemoteFeedbackError {
    data object Network : RemoteFeedbackError

    data object Serialization : RemoteFeedbackError

    data class Unknown(
        val cause: String?,
    ) : RemoteFeedbackError
}
