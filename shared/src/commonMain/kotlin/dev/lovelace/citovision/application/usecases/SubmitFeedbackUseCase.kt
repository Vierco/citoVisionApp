package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.RemoteFeedback
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.Feedback
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/**
 * Envía el feedback del usuario a la base de datos remota (MVP: se revisa desde la consola, sin correo).
 * Completa la marca de tiempo y el `ownerUid` de la sesión activa.
 *
 * Exige **cuenta iniciada**: las reglas de Firestore solo admiten escritura de usuarios autenticados,
 * así que un invitado se rechaza aquí sin gastar una llamada de red que el servidor devolvería con 403.
 */
class SubmitFeedbackUseCase(
    private val remoteFeedback: RemoteFeedback,
    private val authService: AuthService,
) {
    suspend operator fun invoke(
        email: String,
        message: String,
    ): FeedbackResult {
        val user = authService.currentUser.first()
        if (user == null || user.isGuest) {
            return FeedbackResult.REQUIRES_ACCOUNT
        }
        val feedback =
            Feedback(
                email = email.trim(),
                message = message.trim(),
                createdAt = Clock.System.now(),
                ownerUid = user.uid,
            )
        return when (remoteFeedback.submit(feedback)) {
            is Result.Success -> FeedbackResult.SENT
            is Result.Failure -> FeedbackResult.ERROR
        }
    }
}

/** Desenlace del envío de feedback: enviado, rechazado por falta de cuenta o fallo remoto. */
enum class FeedbackResult {
    SENT,
    REQUIRES_ACCOUNT,
    ERROR,
}
