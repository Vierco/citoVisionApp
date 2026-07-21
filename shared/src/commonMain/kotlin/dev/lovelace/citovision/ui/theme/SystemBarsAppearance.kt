package dev.lovelace.citovision.ui.theme

import androidx.compose.runtime.Composable

/**
 * Sincroniza la apariencia (claro/oscuro) de los iconos de las barras del sistema con el tema efectivo de
 * la app. Es necesario cuando el usuario fuerza un tema distinto al del sistema: sin esto, en Android los
 * iconos de estado/navegación siguen al SO y no contrastan con el contenido. En Desktop/iOS es un no-op.
 */
@Composable
expect fun SystemBarsAppearance(darkTheme: Boolean)
