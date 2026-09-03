package dev.lovelace.citovision.infrastructure.image

/** Android tiene selector de fotos (`PickVisualMedia`) y explorador de documentos (SAF) por separado. */
internal actual val platformHasDistinctImageSources: Boolean = true

/** El selector se lanza por `ActivityResultContracts`, sin ventanas intermedias que estorben. */
internal actual val platformCanOpenPickerAfterDialog: Boolean = true
