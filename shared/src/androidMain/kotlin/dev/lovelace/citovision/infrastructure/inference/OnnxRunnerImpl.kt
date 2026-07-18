package dev.lovelace.citovision.infrastructure.inference

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import dev.lovelace.citovision.application.ports.OnnxOutput
import dev.lovelace.citovision.application.ports.OnnxRunner
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.FloatBuffer

/**
 * Ejecuta el modelo ONNX en Android con `ai.onnxruntime` (SPEC-0006, ADR-0003). La `OrtSession` se crea una
 * sola vez (perezosa, protegida por Mutex) a partir de los bytes del modelo y se reutiliza. Se lee la primera
 * salida (rama de detección `output0`, layout `[1, atributos, anclas]`); los prototipos de máscara se ignoran.
 */
class OnnxRunnerImpl(
    private val modelProvider: suspend () -> ByteArray,
) : OnnxRunner {
    private val environment = OrtEnvironment.getEnvironment()
    private val initMutex = Mutex()
    private var session: OrtSession? = null

    override suspend fun run(input: FloatArray): OnnxOutput {
        val ortSession = ensureSession()
        val inputName = ortSession.inputNames.first()
        val shape = longArrayOf(BATCH, CHANNELS, INPUT_SIZE, INPUT_SIZE)
        return OnnxTensor.createTensor(environment, FloatBuffer.wrap(input), shape).use { tensor ->
            ortSession.run(mapOf(inputName to tensor)).use { results ->
                val outputTensor = results.iterator().next().value as OnnxTensor
                val attributes = outputTensor.info.shape[1].toInt()
                val buffer = outputTensor.floatBuffer
                val flat = FloatArray(buffer.remaining())
                buffer.get(flat)
                OnnxOutput(data = flat, attributes = attributes)
            }
        }
    }

    private suspend fun ensureSession(): OrtSession =
        initMutex.withLock {
            session ?: environment.createSession(modelProvider()).also { session = it }
        }

    private companion object {
        const val BATCH = 1L
        const val CHANNELS = 3L
        const val INPUT_SIZE = 640L
    }
}
