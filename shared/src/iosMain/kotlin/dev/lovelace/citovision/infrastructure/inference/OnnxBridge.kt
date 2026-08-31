package dev.lovelace.citovision.infrastructure.inference

import platform.Foundation.NSData

/**
 * Resultado crudo de una ejecución del modelo en el lado nativo (ADR-0007): el tensor de la rama de
 * detección, su número de atributos (`shape[1]`) y, si algo falló, el motivo.
 *
 * Es una clase pequeña y no una lambda de tres parámetros a propósito: los `Int` de un constructor cruzan a
 * Objective-C como `int32_t`, mientras que los de un parámetro de lambda se empaquetarían en `KotlinInt` y
 * Swift tendría que envolverlos.
 */
class OnnxNativeResult(
    val output: NSData?,
    val attributes: Int,
    val error: String?,
)

/**
 * Puente hacia ONNX Runtime, que en iOS vive **solo en Swift** (ADR-0007): el framework compartido no enlaza
 * el runtime, igual que no enlaza el SDK de Google Sign-In (ADR-0006).
 *
 * Swift instala [runner] al arrancar la app (`OnnxRuntimeBridge.install()`). Si no lo hace, `OnnxRunnerImpl`
 * falla de forma controlada y la inferencia se traduce a `InferenceError`, nunca a un crash.
 *
 * Los datos cruzan como [NSData] y no como `FloatArray`: el tensor de entrada son 1 228 800 floats, y
 * recorrer un `KotlinFloatArray` desde Swift costaría más de un millón de envíos de mensaje Objective-C.
 *
 * La llamada es **síncrona**: `CellDetectorImpl` ya ejecuta la inferencia fuera del hilo principal, en el
 * dispatcher inyectado (SPEC-0006 RNF).
 */
object OnnxBridge {
    var runner: ((modelPath: String, input: NSData) -> OnnxNativeResult)? = null
}
