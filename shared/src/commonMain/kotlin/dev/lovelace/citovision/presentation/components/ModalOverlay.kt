package dev.lovelace.citovision.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Cuántas superficies modales hay abiertas ahora mismo. Es un contador y no un booleano porque los
 * diálogos se anidan (el detalle de un análisis puede abrir la imagen a pantalla completa).
 */
internal object ModalOverlayState {
    var count by mutableStateOf(0)
        private set

    fun enter() {
        count++
    }

    fun exit() {
        count--
    }
}

/**
 * Declara que el composable que lo invoca es una **superficie modal**, mientras esté en pantalla.
 *
 * Solo tiene efecto en iOS, donde la barra de pestañas la dibuja SwiftUI **por encima** de Compose
 * (ADR-0008) y por tanto flotaría también sobre los diálogos. Al marcarlos, la barra se aparta y se
 * recupera el comportamiento de Android y Desktop, donde el diálogo tapa la barra de forma natural.
 *
 * **Llámalo desde dentro del propio diálogo**, no desde donde se decide mostrarlo: así queda atado a su
 * ciclo de vida y funciona desde cualquier sitio que lo use.
 */
@Composable
fun ModalOverlayEffect() {
    DisposableEffect(Unit) {
        ModalOverlayState.enter()
        onDispose { ModalOverlayState.exit() }
    }
}
