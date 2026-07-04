package dev.lovelace.citovision.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lovelace.citovision.presentation.navigation.NavigationEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    fun onLoginClick() {
        viewModelScope.launch { _navigationEvents.send(NavigationEvent.ToMain) }
    }

    fun onGoogleLoginClick() {
        viewModelScope.launch { _navigationEvents.send(NavigationEvent.ToMain) }
    }

    fun onGuestClick() {
        viewModelScope.launch { _navigationEvents.send(NavigationEvent.ToMain) }
    }
}
