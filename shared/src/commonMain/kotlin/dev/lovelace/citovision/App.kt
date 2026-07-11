package dev.lovelace.citovision

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
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
        // ImageLoader único con fetcher de red (Coil 3 no trae uno por defecto): habilita las imágenes
        // remotas de Storage (SPEC-0005). El fetcher usa su propio HttpClient Ktor sobre el engine de plataforma.
        setSingletonImageLoaderFactory { context ->
            ImageLoader
                .Builder(context)
                .components { add(KtorNetworkFetcherFactory()) }
                .build()
        }
        CitoVisionTheme {
            AppNavHost()
        }
    }
}
