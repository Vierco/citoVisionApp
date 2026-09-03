package dev.lovelace.citovision.presentation.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Una pestaña de la barra inferior: su etiqueta ya traducida y el icono que la representa. */
class AppNavigationItem(
    val label: String,
    val icon: Painter,
)

/**
 * Barra de navegación inferior de la app, con el aspecto propio de cada plataforma (ADR-0008).
 *
 * - **Android y Desktop** dibujan el `NavigationBar` de Material 3 ([MaterialAppNavigationBar]).
 * - **iOS** no dibuja nada: publica su estado al lado Swift, que pinta una barra flotante nativa
 *   *por encima* de Compose, y aquí solo se reserva el hueco equivalente.
 *
 * El estado sigue viviendo en Compose: quien la usa mantiene [selectedIndex] y reacciona a [onSelect].
 */
@Composable
expect fun AppNavigationBar(
    items: List<AppNavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
)

/**
 * Insets que el `Scaffold` principal reserva para su contenido.
 *
 * En iOS **se excluye el borde inferior**: la barra de pestañas flota sobre el contenido (ADR-0008), así
 * que la lista debe llegar hasta abajo del todo y pasar **por debajo** del cristal, como en cualquier app
 * nativa. En Android y Desktop se mantiene el comportamiento por defecto, porque allí la barra ocupa su
 * propio hueco.
 */
@Composable
expect fun appScaffoldContentInsets(): WindowInsets

/**
 * Espacio que hay que añadir al **final** de un contenido desplazable para que su último elemento pueda
 * quedar por encima de una barra flotante. Vale `0` salvo en iOS.
 */
@Composable
expect fun floatingNavigationBarPadding(): Dp

/**
 * Implementación Material 3, compartida por los `actual` de Android y Desktop para no duplicarla en dos
 * *source sets*. Fondo transparente porque el degradado lo pinta la pantalla que la contiene.
 *
 * Usa **`ShortNavigationBar`**, la barra de navegación de **Material 3 Expressive**, en lugar del
 * `NavigationBar` clásico, que es el componente al que Material da continuidad.
 *
 * Conviene no esperar de ella más de lo que da: con los parámetros por defecto el indicador activo y la
 * disposición del ítem son **idénticos**, y lo único que cambia es el alto del contenedor (64 dp frente a
 * los 80 dp de `TallContainerHeight`). Su valor real está en dos capacidades que el componente antiguo no
 * tiene y que aquí **no se usan**: `arrangement` (`Centered` agrupa las pestañas, recomendado solo para
 * pantallas medianas) e `iconPosition` (`Start` pone el icono junto a la etiqueta).
 *
 * Material mantiene además a propósito la navegación **anclada al borde inferior y a todo el ancho**, al
 * contrario que iOS 26, que la despega (ADR-0008).
 */
@Composable
internal fun MaterialAppNavigationBar(
    items: List<AppNavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    ShortNavigationBar(containerColor = Color.Transparent) {
        items.forEachIndexed { index, item ->
            ShortNavigationBarItem(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                icon = {
                    Icon(
                        painter = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(ICON_SIZE),
                    )
                },
                label = { Text(item.label) },
                colors =
                    ShortNavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondary,
                        selectedIndicatorColor = MaterialTheme.colorScheme.secondary,
                    ),
            )
        }
    }
}

private val ICON_SIZE = 24.dp
