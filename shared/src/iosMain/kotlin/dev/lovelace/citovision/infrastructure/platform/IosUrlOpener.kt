package dev.lovelace.citovision.infrastructure.platform

import dev.lovelace.citovision.application.ports.UrlOpener
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * Abre una URL en Safari mediante `UIApplication`. iOS no forma parte del MVP; implementación mínima para
 * mantener la paridad de plataformas y que el módulo compile.
 */
class IosUrlOpener : UrlOpener {
    override fun open(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(nsUrl)
    }
}
