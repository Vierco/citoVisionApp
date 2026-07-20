package dev.lovelace.citovision

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.lovelace.citovision.composition.di.initKoin
import dev.lovelace.citovision.config.DesktopBuildConfig
import dev.lovelace.citovision.infrastructure.remote.FIREBASE_WEB_API_KEY_PROPERTY
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import java.awt.Taskbar
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

private const val APP_ICON_RESOURCE = "icons/citovision.png"

/**
 * Icono de la aplicación, compartido con el launcher de Android para que la identidad sea la misma en
 * ambas plataformas. Devuelve `null` si el recurso falta, en cuyo caso se cae al icono por defecto de la
 * JVM: un icono ausente no debe impedir arrancar.
 */
private fun loadAppIcon(): BufferedImage? =
    Thread
        .currentThread()
        .contextClassLoader
        ?.getResourceAsStream(APP_ICON_RESOURCE)
        ?.use(ImageIO::read)

/**
 * En macOS el icono del Dock no lo fija el parámetro `icon` de la ventana, sino AWT a través de [Taskbar];
 * sin esto sale la taza de Java. En Windows y Linux la llamada no está soportada y se ignora, porque ahí
 * el icono de la barra de tareas sí lo pone la ventana.
 */
private fun applyDockIcon(icon: BufferedImage) {
    if (!Taskbar.isTaskbarSupported()) return
    val taskbar = Taskbar.getTaskbar()
    if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
        taskbar.iconImage = icon
    }
}

fun main() {
    // Nombre mostrado en la barra de menús de macOS (por defecto tomaría el nombre del proceso JVM).
    System.setProperty("apple.awt.application.name", "citoVision")
    val appIcon = loadAppIcon()
    appIcon?.let(::applyDockIcon)
    Napier.base(DebugAntilog())
    initKoin {
        properties(mapOf(FIREBASE_WEB_API_KEY_PROPERTY to DesktopBuildConfig.FIREBASE_WEB_API_KEY))
    }
    application {
        val windowState =
            rememberWindowState(
                size = DpSize(width = 900.dp, height = 1000.dp),
                position = WindowPosition(Alignment.Center),
            )
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "citoVision",
            icon = appIcon?.let { BitmapPainter(it.toComposeImageBitmap()) },
        ) {
            App()
        }
    }
}
