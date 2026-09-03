package dev.lovelace.citovision.infrastructure.image

/** En Desktop hay un único diálogo de fichero que ya llega a todo el disco: no hay nada que elegir. */
internal actual val platformHasDistinctImageSources: Boolean = false

/** Diálogo AWT nativo: no hay ninguna ventana de Compose que se esté desmontando de por medio. */
internal actual val platformCanOpenPickerAfterDialog: Boolean = true
