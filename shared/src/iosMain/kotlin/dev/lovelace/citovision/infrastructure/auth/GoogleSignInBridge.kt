package dev.lovelace.citovision.infrastructure.auth

/**
 * Puente hacia el flujo nativo de Google Sign-In en iOS (ADR-0006).
 *
 * Kotlin **no** enlaza el SDK de Google: lo hace Swift, que en el arranque instala aquí su lanzador. Así
 * el framework compartido no arrastra dependencias nativas y se evita el conflicto de enlazado que
 * describe el ADR.
 *
 * Solo se usa desde el hilo principal: Swift lo instala al arrancar y el ViewModel lo invoca desde la UI.
 */
object GoogleSignInBridge {
    /**
     * Lanzador nativo instalado por Swift. Recibe un callback que debe invocarse **exactamente una vez**:
     * con el ID token de Google y `null`, o con `null` y el motivo del fallo ([CANCELLED] si el usuario
     * cerró el selector, cualquier otro texto para un error real).
     *
     * Mientras valga `null` (Swift no lo ha instalado), el login con Google falla de forma controlada.
     */
    var presenter: ((onResult: (idToken: String?, error: String?) -> Unit) -> Unit)? = null

    /** Motivo que Swift envía cuando el usuario cancela; debe coincidir literalmente con el lado Swift. */
    const val CANCELLED: String = "cancelled"
}
