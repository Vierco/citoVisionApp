package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.RemoteAnalysisSync
import dev.lovelace.citovision.core.result.Result

/**
 * Drena el outbox de sincronización (SPEC-0005, RN-8). Lo usan el botón "Reintentar" del popup y el
 * disparo al reabrir la app. Devuelve `true` si todo quedó sincronizado; `false` si algo sigue pendiente.
 */
class ProcessPendingSyncUseCase(
    private val remoteAnalysisSync: RemoteAnalysisSync,
) {
    suspend operator fun invoke(): Boolean =
        when (remoteAnalysisSync.processPending()) {
            is Result.Success -> true
            is Result.Failure -> false
        }
}
