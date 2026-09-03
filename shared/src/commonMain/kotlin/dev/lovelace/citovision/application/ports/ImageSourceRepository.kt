package dev.lovelace.citovision.application.ports

import dev.lovelace.citovision.domain.settings.ImageSourcePreference
import kotlinx.coroutines.flow.Flow

/**
 * Persistencia de la fuente de imágenes elegida en Ajustes (SPEC-0003). Dato no sensible en DataStore;
 * el acceso a DataStore vive solo en Infrastructure.
 *
 * Guarda además si ya se mostró el **aviso único**: la primera vez que el usuario abre el selector se
 * le explica una sola vez que puede cambiar la fuente en Ajustes, para que la preferencia no quede
 * escondida. Es un aviso informativo, no una elección: no altera la preferencia.
 */
interface ImageSourceRepository {
    fun imageSource(): Flow<ImageSourcePreference>

    suspend fun setImageSource(preference: ImageSourcePreference)

    /** `true` mientras el aviso siga pendiente de mostrarse. */
    fun isSourceNoticePending(): Flow<Boolean>

    suspend fun markSourceNoticeShown()
}
