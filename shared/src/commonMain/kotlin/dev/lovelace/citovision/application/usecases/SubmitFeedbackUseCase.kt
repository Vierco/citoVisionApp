package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.RemoteFeedback
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.Feedback
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/**
 * Envía el feedback del usuario a la base de datos remota (MVP: se revisa desde la consola, sin correo).
 * Completa la marca de tiempo y el `ownerUid` de la sesión activa (nulo si es invitado). Devuelve `true`
 * si el envío se completó.
 */
class SubmitFeedbackUseCase(
    private val remoteFeedback: RemoteFeedback,
    private val authService: AuthService,
) {
    suspend operator fun invoke(
        email: String,
        message: String,
    ): Boolean {
        val ownerUid = authService.currentUser.first()?.takeUnless { it.isGuest }?.uid
        val feedback =
            Feedback(
                email = email.trim(),
                message = message.trim(),
                createdAt = Clock.System.now(),
                ownerUid = ownerUid,
            )
        return when (remoteFeedback.submit(feedback)) {
            is Result.Success -> true
            is Result.Failure -> false
        }
    }
}
