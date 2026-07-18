package dev.lovelace.citovision.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Dispatcher para trabajo intensivo de CPU (inferencia on-device, SPEC-0006). Separado de [ioDispatcher]:
 * la inferencia no es E/S, así que se ejecuta en el pool de cómputo (`Dispatchers.Default`), no en el de E/S.
 */
expect val defaultDispatcher: CoroutineDispatcher
