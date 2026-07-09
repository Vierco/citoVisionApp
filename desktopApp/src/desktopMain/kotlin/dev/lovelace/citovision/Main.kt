package dev.lovelace.citovision

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.lovelace.citovision.composition.di.initKoin

fun main() {
    initKoin()
    application {
        Window(onCloseRequest = ::exitApplication, title = "citoVision") {
            App()
        }
    }
}
