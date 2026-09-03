package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.ImagePicker
import dev.lovelace.citovision.application.ports.ImageSourceRepository
import dev.lovelace.citovision.domain.settings.ImageSourcePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Observa la fuente de imágenes elegida en Ajustes (fototeca o ficheros). */
class ObserveImageSourceUseCase(
    private val repository: ImageSourceRepository,
) {
    operator fun invoke(): Flow<ImageSourcePreference> = repository.imageSource()
}

/** Guarda la fuente de imágenes elegida por el usuario. */
class SetImageSourceUseCase(
    private val repository: ImageSourceRepository,
) {
    suspend operator fun invoke(preference: ImageSourcePreference) = repository.setImageSource(preference)
}

/**
 * Si hay que mostrar el aviso único que explica dónde se cambia la fuente.
 *
 * Combina las dos condiciones para que ninguna pantalla tenga que recordarlas: que el aviso siga
 * pendiente **y** que la plataforma tenga de verdad dos selectores distintos. En Desktop siempre es
 * `false`, porque allí no hay nada que elegir ni, por tanto, nada que explicar.
 */
class ObserveImageSourceNoticeUseCase(
    private val repository: ImageSourceRepository,
    private val imagePicker: ImagePicker,
) {
    operator fun invoke(): Flow<Boolean> =
        if (imagePicker.hasDistinctSources) repository.isSourceNoticePending() else flowOf(false)
}

/** Marca el aviso como visto para que no vuelva a aparecer. */
class MarkImageSourceNoticeShownUseCase(
    private val repository: ImageSourceRepository,
) {
    suspend operator fun invoke() = repository.markSourceNoticeShown()
}

/**
 * Si la plataforma ofrece elegir entre fototeca y ficheros. Decide si Ajustes muestra la sección; en
 * Desktop no se pinta, porque su diálogo de fichero ya llega a todo.
 */
class ImageSourceOptionsAvailableUseCase(
    private val imagePicker: ImagePicker,
) {
    operator fun invoke(): Boolean = imagePicker.hasDistinctSources
}

/**
 * Si al cerrar el aviso se puede abrir el selector en la misma acción. En iOS no (ver
 * [ImagePicker.canOpenPickerAfterDialog]), y allí el aviso pide al usuario que vuelva a pulsar.
 */
class CanOpenPickerAfterNoticeUseCase(
    private val imagePicker: ImagePicker,
) {
    operator fun invoke(): Boolean = imagePicker.canOpenPickerAfterDialog
}
