package dev.lovelace.citovision.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lovelace.citovision.presentation.navigation.NavigationEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SplashViewModel : ViewModel() {

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            delay(SPLASH_DELAY_MS)
            _navigationEvents.send(NavigationEvent.ToLogin)
        }
    }

    private companion object {
        const val SPLASH_DELAY_MS = 2000L
    }
}
