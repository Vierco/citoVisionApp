package dev.lovelace.citovision

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import dev.lovelace.citovision.application.usecases.ObserveThemePreferenceUseCase
import dev.lovelace.citovision.domain.settings.ThemePreference
import dev.lovelace.citovision.presentation.navigation.AppNavHost
import dev.lovelace.citovision.ui.theme.CitoVisionTheme
import dev.lovelace.citovision.ui.theme.SystemBarsAppearance
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

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
        // La preferencia de tema (Claro/Oscuro/Seguir sistema) se persiste en DataStore; en `SYSTEM` sigue
        // al sistema. Se lee aquí, en la raíz, para elegir el esquema antes de dibujar la UI.
        val observeThemePreference = koinInject<ObserveThemePreferenceUseCase>()
        val themePreference by observeThemePreference().collectAsStateWithLifecycle(ThemePreference.SYSTEM)
        val darkTheme =
            when (themePreference) {
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
            }
        // Iconos de las barras del sistema acordes al tema efectivo (Android; no-op en Desktop/iOS).
        SystemBarsAppearance(darkTheme = darkTheme)
        CitoVisionTheme(darkTheme = darkTheme) {
            AppNavHost()
        }
    }
}
