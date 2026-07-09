package dev.lovelace.citovision

import androidx.compose.runtime.Composable
import dev.lovelace.citovision.presentation.navigation.AppNavHost
import dev.lovelace.citovision.ui.theme.CitoVisionTheme
import org.koin.compose.KoinContext

/**
 * Raíz de la UI compartida. Koin se arranca antes en cada entry point de plataforma
 * (Application en Android, fun main en Desktop) vía initKoin(); aquí solo se expone
 * el contexto de Koin ya iniciado a los Composables.
 */
@Composable
fun App() {
    KoinContext {
        CitoVisionTheme {
            AppNavHost()
        }
    }
}
