package dev.lovelace.citovision.presentation.components

/**
 * Estado que Compose publica hacia la barra nativa (ADR-0008): si debe verse, las etiquetas ya traducidas
 * y cuál está activa.
 *
 * Es una clase y no tres parámetros sueltos de lambda a propósito, por la misma razón que
 * `OnnxNativeResult` (ADR-0007): los primitivos de un **constructor** cruzan a Objective-C como `Bool` e
 * `int32_t`, mientras que los de un **parámetro de lambda** se empaquetarían en `KotlinBoolean` y
 * `KotlinInt`, y Swift tendría que desenvolverlos.
 */
class NativeTabBarState(
    val visible: Boolean,
    val labels: List<String>,
    val selectedIndex: Int,
)

/**
 * Puente con la barra de pestañas nativa, que en iOS dibuja SwiftUI **por encima** de Compose (ADR-0008).
 *
 * Es bidireccional, pero el estado **no se duplica**: la fuente única de verdad sigue siendo el
 * `selectedTabIndex` de `MainScreen`. Swift solo pinta lo que se le publica y avisa de los toques.
 *
 * Que la barra se vea o no se deduce del ciclo de vida de la composición, así que desaparece sola en
 * Splash, Login y Ajustes sin lógica añadida.
 */
object NativeTabBarBridge {
    /** Lo instala Swift al arrancar para enterarse de los cambios. */
    var onStateChanged: ((NativeTabBarState) -> Unit)? = null

    /** Lo llama Swift cuando el usuario toca una pestaña. */
    fun selectTab(index: Int) {
        tabSelection?.invoke(index)
    }

    /** Lo rellena `AppNavigationBar` mientras está en pantalla; `internal`, así que no se exporta a Swift. */
    internal var tabSelection: ((Int) -> Unit)? = null

    internal fun publish(state: NativeTabBarState) {
        onStateChanged?.invoke(state)
    }
}
