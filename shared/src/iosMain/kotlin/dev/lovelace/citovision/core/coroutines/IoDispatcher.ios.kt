package dev.lovelace.citovision.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * En Kotlin/Native no hay `Dispatchers.IO` público. `Dispatchers.Default` usa un pool multihilo, que es
 * suficiente para la E/S puntual de esta app (SQLite y ficheros de imagen).
 */
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
