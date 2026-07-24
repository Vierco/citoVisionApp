package dev.lovelace.citovision.infrastructure.auth

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.lovelace.citovision.application.ports.AuthTokenProvider
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException

/**
 * ID token del usuario de Firebase en Android (GitLive). `getIdToken(false)` devuelve el token en caché
 * y lo renueva por su cuenta cuando ha caducado, así que aquí no hay que gestionar la expiración.
 * El invitado es local (no crea cuenta Firebase, SPEC-0001 RN-3), luego no hay `currentUser` ni token.
 */
class FirebaseAuthTokenProvider : AuthTokenProvider {
    override suspend fun currentIdToken(): String? =
        try {
            Firebase.auth.currentUser?.getIdToken(false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Nunca se loguea el token, solo que no se pudo obtener (AGENTS.md §11).
            Napier.w("No se pudo obtener el ID token de Firebase", e, tag = "Auth")
            null
        }
}
