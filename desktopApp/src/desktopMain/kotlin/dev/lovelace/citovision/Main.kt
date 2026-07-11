package dev.lovelace.citovision

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.lovelace.citovision.composition.di.initKoin
import dev.lovelace.citovision.config.DesktopBuildConfig
import dev.lovelace.citovision.infrastructure.remote.FIREBASE_WEB_API_KEY_PROPERTY
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

fun main() {
    Napier.base(DebugAntilog())
    initKoin {
        properties(mapOf(FIREBASE_WEB_API_KEY_PROPERTY to DesktopBuildConfig.FIREBASE_WEB_API_KEY))
    }
    application {
        Window(onCloseRequest = ::exitApplication, title = "citoVision") {
            App()
        }
    }
}
