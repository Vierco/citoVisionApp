package dev.lovelace.citovision.presentation.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * En iOS la barra la dibuja SwiftUI **por encima** de Compose (ADR-0008), así que aquí no se pinta nada
 * **ni se reserva hueco**: la barra flota y el contenido pasa por debajo del cristal, como es propio de
 * iOS. Quien tenga que apartar su último elemento usa [floatingNavigationBarPadding].
 *
 * La visibilidad se deduce del ciclo de vida: se anuncia al entrar en composición y se retira al salir.
 * Como esta barra solo se usa en `MainScreen`, desaparece sola en Splash, Login y Ajustes; y se aparta
 * también mientras haya un diálogo abierto ([ModalOverlayEffect]), que en las otras plataformas la taparía.
 */
@Composable
actual fun AppNavigationBar(
    items: List<AppNavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    // El puente guarda la referencia mientras la barra vive; `rememberUpdatedState` evita que se quede
    // apuntando a un callback viejo cuando `MainScreen` recompone.
    val currentOnSelect by rememberUpdatedState(onSelect)
    val labels = items.map { it.label }
    val coveredByModal = ModalOverlayState.count > 0

    DisposableEffect(Unit) {
        NativeTabBarBridge.tabSelection = { index -> currentOnSelect(index) }
        onDispose {
            NativeTabBarBridge.tabSelection = null
            NativeTabBarBridge.publish(NativeTabBarState(visible = false, labels = emptyList(), selectedIndex = 0))
        }
    }

    // `labels` se recrea en cada recomposición, pero la comparación de claves es estructural: mientras el
    // contenido no cambie, no se vuelve a publicar.
    LaunchedEffect(labels, selectedIndex, coveredByModal) {
        NativeTabBarBridge.publish(
            NativeTabBarState(visible = !coveredByModal, labels = labels, selectedIndex = selectedIndex),
        )
    }
}

/** Sin borde inferior: el contenido llega hasta abajo y la barra flotante se superpone. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun appScaffoldContentInsets(): WindowInsets =
    ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)

/**
 * Alto de la cápsula más el margen inferior seguro, que ahora también invade el contenido.
 *
 * La parte de la cápsula **debe cuadrar con el layout de `NativeTabBar.swift`**: si cambia una y no la
 * otra, el último elemento de las listas queda medio tapado (deuda conocida, ADR-0008).
 */
@Composable
actual fun floatingNavigationBarPadding(): Dp =
    BAR_HEIGHT + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

private val BAR_HEIGHT = 64.dp
