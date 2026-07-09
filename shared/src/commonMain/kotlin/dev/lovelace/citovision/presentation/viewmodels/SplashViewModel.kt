package dev.lovelace.citovision.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lovelace.citovision.application.usecases.HasActiveSessionUseCase
import dev.lovelace.citovision.presentation.navigation.NavigationEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Comprueba si hay sesión activa (cuenta Firebase o invitado persistido) y decide el destino
 * (SPEC-0001 RF-8, CA-9). Mantiene un tiempo mínimo de marca mientras resuelve la sesión en paralelo.
 */
class SplashViewModel(
    private val hasActiveSession: HasActiveSessionUseCase,
) : ViewModel() {
    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            val sessionCheck = async { hasActiveSession() }
            delay(SPLASH_DELAY_MS)
            val destination =
                if (sessionCheck.await()) NavigationEvent.ToMain else NavigationEvent.ToLogin
            _navigationEvents.send(destination)
        }
    }

    private companion object {
        const val SPLASH_DELAY_MS = 2000L
    }
}
