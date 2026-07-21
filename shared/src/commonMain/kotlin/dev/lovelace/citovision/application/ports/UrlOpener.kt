package dev.lovelace.citovision.application.ports

/**
 * Puerto que abre una URL en el navegador del sistema. La forma de lanzar el navegador es específica de
 * plataforma (Android: `Intent.ACTION_VIEW`; Desktop: `java.awt.Desktop`; iOS: `UIApplication`), por eso
 * vive tras esta abstracción y se inyecta vía Koin desde el módulo de plataforma.
 */
interface UrlOpener {
    /** Abre [url] en el navegador. Los fallos se registran y se ignoran; no interrumpen la UI. */
    fun open(url: String)
}
