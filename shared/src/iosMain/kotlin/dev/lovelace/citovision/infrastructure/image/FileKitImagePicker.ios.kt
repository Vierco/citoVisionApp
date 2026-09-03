package dev.lovelace.citovision.infrastructure.image

/** iOS separa la fototeca (`PHPickerViewController`) de la app Archivos (`UIDocumentPickerViewController`). */
internal actual val platformHasDistinctImageSources: Boolean = true

/**
 * En iOS **no**. El diálogo de Compose vive en una `UIWindow` a nivel alerta y FileKit presenta sobre la
 * *key window*: pedir el selector mientras esa ventana se desmonta lo deja presentado sobre algo que
 * desaparece, y su `suspendCoroutine` no se reanuda jamás. Hay que esperar a otra pulsación.
 */
internal actual val platformCanOpenPickerAfterDialog: Boolean = false
