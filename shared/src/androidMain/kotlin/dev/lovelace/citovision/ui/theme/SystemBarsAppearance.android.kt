package dev.lovelace.citovision.ui.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Fija la apariencia de los iconos de las barras del sistema: claros cuando el tema es oscuro y viceversa.
 * `enableEdgeToEdge()` los ajusta según el modo del SO, así que aquí se sobreescribe con el tema efectivo
 * de la app para que contrasten también cuando el usuario fuerza un tema distinto al del sistema.
 */
@Composable
actual fun SystemBarsAppearance(darkTheme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        SideEffect {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
}
