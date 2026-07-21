package dev.lovelace.citovision.infrastructure.platform

import dev.lovelace.citovision.application.ports.UrlOpener
import io.github.aakira.napier.Napier
import java.awt.Desktop
import java.net.URI

/** Abre una URL con el navegador por defecto del escritorio mediante `java.awt.Desktop`. */
class DesktopUrlOpener : UrlOpener {
    override fun open(url: String) {
        runCatching {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
            } else {
                Napier.w("El escritorio no soporta abrir el navegador")
            }
        }.onFailure { Napier.w("No se pudo abrir la URL en el navegador", it) }
    }
}
