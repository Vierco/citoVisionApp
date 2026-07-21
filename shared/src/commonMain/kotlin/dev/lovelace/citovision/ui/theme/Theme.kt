package dev.lovelace.citovision.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colores del sistema de diseño que Material3 no cubre con un slot de `ColorScheme` (estados pressed,
 * semánticos success/warning/info, `hint` y `secondaryDark`). Se exponen vía `CompositionLocal` para que
 * cambien con el tema. Los slots que sí están en `ColorScheme` (primary, error, etc.) se leen de
 * `MaterialTheme.colorScheme`, no de aquí.
 */
data class AppExtendedColors(
    val primaryPressed: Color,
    val secondaryPressed: Color,
    val secondaryDark: Color,
    val hint: Color,
    val success: Color,
    val warning: Color,
    val errorPressed: Color,
    val info: Color,
)

private val LightExtendedColors =
    AppExtendedColors(
        primaryPressed = primaryPressed,
        secondaryPressed = secondaryPressed,
        secondaryDark = secondaryDark,
        hint = hint,
        success = success,
        warning = warning,
        errorPressed = errorPressed,
        info = info,
    )

private val DarkExtendedColors =
    AppExtendedColors(
        primaryPressed = darkPrimaryPressed,
        secondaryPressed = darkSecondaryPressed,
        secondaryDark = darkSecondaryDark,
        hint = darkHint,
        success = darkSuccess,
        warning = darkWarning,
        errorPressed = darkErrorPressed,
        info = darkInfo,
    )

/** Por defecto usa la paleta clara (evita que las previews sin `CitoVisionTheme` fallen). */
val LocalAppColors = staticCompositionLocalOf { LightExtendedColors }

private val LightColorScheme =
    lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        tertiary = tertiary,
        onTertiary = onTertiary,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        error = error,
        onError = Color.White,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = darkPrimary,
        onPrimary = darkOnPrimary,
        secondary = darkSecondary,
        onSecondary = darkOnSecondary,
        tertiary = darkTertiary,
        onTertiary = darkOnTertiary,
        background = darkBackground,
        onBackground = darkOnBackground,
        surface = darkSurface,
        onSurface = darkOnSurface,
        error = darkError,
        onError = Color.White,
    )

@Composable
fun CitoVisionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = getTypography(),
    ) {
        CompositionLocalProvider(
            LocalAppColors provides extendedColors,
            // Sin un Surface envolvente el color de contenido por defecto sería negro y los iconos/textos
            // sin color explícito quedarían invisibles en oscuro. Se fija al onBackground del tema.
            LocalContentColor provides colorScheme.onBackground,
        ) {
            content()
        }
    }
}
