package dev.lovelace.citovision.infrastructure.inference

import dev.lovelace.citovision.application.ports.OnnxOutput
import dev.lovelace.citovision.application.ports.OnnxRunner

/**
 * Stub de iOS (SPEC-0006): la ejecución ONNX en iOS se implementará con cinterop (ONNX Runtime C) en una fase
 * futura. Mantiene la misma firma que las variantes JVM para que la inyección de dependencias sea uniforme.
 */
class OnnxRunnerImpl(
    modelProvider: suspend () -> ByteArray,
) : OnnxRunner {
    override suspend fun run(input: FloatArray): OnnxOutput =
        throw NotImplementedError("Inferencia ONNX en iOS pendiente (cinterop, fase futura)")
}
