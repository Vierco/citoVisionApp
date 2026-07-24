package dev.lovelace.citovision.application.ports

/**
 * Provee el **ID token** de Firebase del usuario con cuenta para autorizar las llamadas REST a Firestore
 * y Storage (ADR-0001: la autorización efectiva vive en las reglas del servidor, no en el cliente).
 *
 * Devuelve `null` cuando no hay sesión de cuenta (invitado o sin login) o cuando el token no ha podido
 * renovarse; en ese caso la petición sale sin `Authorization` y el servidor la rechaza. Lo implementa
 * cada plataforma: Firebase/GitLive en Android, la sesión en memoria de Identity Toolkit en Desktop
 * (ADR-0002) y un stub sin token en iOS.
 */
interface AuthTokenProvider {
    suspend fun currentIdToken(): String?
}
