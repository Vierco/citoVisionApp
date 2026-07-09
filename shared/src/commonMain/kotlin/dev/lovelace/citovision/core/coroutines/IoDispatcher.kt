package dev.lovelace.citovision.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Dispatcher para operaciones de E/S (base de datos, ficheros).
 *
 * No se puede usar `Dispatchers.IO` directamente desde `commonMain`: en Kotlin/Native es `internal`
 * (solo existe públicamente en JVM/Android). Por eso se resuelve por plataforma.
 */
expect val ioDispatcher: CoroutineDispatcher
