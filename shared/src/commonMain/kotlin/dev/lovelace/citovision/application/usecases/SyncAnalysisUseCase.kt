package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.RemoteAnalysisSync
import dev.lovelace.citovision.core.result.Result
import kotlinx.coroutines.flow.first

/**
 * Sincroniza un análisis recién guardado con remoto (SPEC-0005). Solo si hay **cuenta iniciada** (RN-5):
 * un invitado guarda solo en local, de forma silenciosa. Con cuenta, encola en el outbox y lanza el
 * proceso; devuelve [SyncOutcome] para que la UI muestre el popup de reintento si falla (RN-8).
 */
class SyncAnalysisUseCase(
    private val authService: AuthService,
    private val remoteAnalysisSync: RemoteAnalysisSync,
) {
    suspend operator fun invoke(analysisId: String): SyncOutcome {
        val user = authService.currentUser.first()
        if (user == null || user.isGuest) {
            return SyncOutcome.SkippedGuest
        }
        remoteAnalysisSync.enqueue(analysisId, user.uid)
        return when (remoteAnalysisSync.processPending()) {
            is Result.Success -> SyncOutcome.Synced
            is Result.Failure -> SyncOutcome.Failed
        }
    }
}

enum class SyncOutcome {
    Synced,
    Failed,
    SkippedGuest,
}
