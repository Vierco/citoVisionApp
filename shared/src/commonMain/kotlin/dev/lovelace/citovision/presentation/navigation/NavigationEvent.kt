package dev.lovelace.citovision.presentation.navigation

sealed interface NavigationEvent {
    data object ToLogin : NavigationEvent

    data object ToMain : NavigationEvent

    data object ToSettings : NavigationEvent

    data object Back : NavigationEvent
}
