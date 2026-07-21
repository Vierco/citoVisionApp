package dev.lovelace.citovision.infrastructure.platform

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import dev.lovelace.citovision.application.ports.UrlOpener
import io.github.aakira.napier.Napier

/**
 * Abre una URL con el navegador del sistema mediante un `Intent.ACTION_VIEW`. Usa el contexto de
 * aplicación con `FLAG_ACTIVITY_NEW_TASK` para no depender de una Activity concreta.
 */
class AndroidUrlOpener(
    private val context: Context,
) : UrlOpener {
    override fun open(url: String) {
        runCatching {
            val intent =
                Intent(Intent.ACTION_VIEW, url.toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }.onFailure { Napier.w("No se pudo abrir la URL en el navegador", it) }
    }
}
