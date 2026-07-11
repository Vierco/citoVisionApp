package dev.lovelace.citovision

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.lovelace.citovision.composition.di.initKoin
import dev.lovelace.citovision.config.DesktopBuildConfig
import dev.lovelace.citovision.infrastructure.remote.FIREBASE_WEB_API_KEY_PROPERTY
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

fun main() {
    // Nombre mostrado en la barra de menús de macOS (por defecto tomaría el nombre del proceso JVM).
    System.setProperty("apple.awt.application.name", "citoVision")
    Napier.base(DebugAntilog())
    initKoin {
        properties(mapOf(FIREBASE_WEB_API_KEY_PROPERTY to DesktopBuildConfig.FIREBASE_WEB_API_KEY))
    }
    application {
        val windowState =
            rememberWindowState(
                size = DpSize(width = 900.dp, height = 1000.dp),
                position = WindowPosition(Alignment.Center),
            )
        Window(onCloseRequest = ::exitApplication, state = windowState, title = "citoVision") {
            App()
        }
    }
}
