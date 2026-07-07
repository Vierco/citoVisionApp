package dev.lovelace.citovision

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import dev.lovelace.citovision.composition.di.presentationModule
import dev.lovelace.citovision.presentation.navigation.AppNavHost
import dev.lovelace.citovision.ui.theme.CitoVisionTheme
import org.koin.compose.KoinApplication

@Composable
fun App() {
    KoinApplication(application = {
        modules(presentationModule)
    }) {
        CitoVisionTheme {
            AppNavHost()
        }
    }
}
