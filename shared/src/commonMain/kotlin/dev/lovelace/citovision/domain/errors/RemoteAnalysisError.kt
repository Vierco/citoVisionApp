package dev.lovelace.citovision.domain.errors

/**
 * Errores del acceso remoto de análisis (SPEC-0005: Firestore/Storage por REST). Cerrado.
 * Infrastructure traduce las excepciones de red y de serialización a estos casos.
 *
 * La consulta sin resultados NO es un error: se representa como lista vacía (RF-6). [Unauthorized] es el
 * 401/403 de unas reglas ya cerradas: sesión ausente, ID token no renovable o dato de otro `ownerUid`.
 */
sealed interface RemoteAnalysisError {
    data object Network : RemoteAnalysisError

    data object Unauthorized : RemoteAnalysisError

    data object Serialization : RemoteAnalysisError

    data class Unknown(
        val cause: String?,
    ) : RemoteAnalysisError
}
