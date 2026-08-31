package dev.lovelace.citovision.infrastructure.inference

import dev.lovelace.citovision.application.ports.OnnxOutput
import dev.lovelace.citovision.application.ports.OnnxRunner
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.posix.memcpy

/**
 * Ejecuta el modelo ONNX en iOS delegando en ONNX Runtime, que vive **solo en Swift** (ADR-0007). Kotlin
 * aporta la ruta del modelo y el tensor ya preprocesado, y recibe de vuelta la rama de detección.
 *
 * Mantiene la misma firma que las variantes JVM —incluido el `modelProvider`— para que el registro en Koin
 * sea idéntico en las tres plataformas. Aquí los bytes solo se leen en el camino de reserva descrito en
 * [resolveModelPath].
 *
 * La `ORTSession` se crea una sola vez y se cachea en el lado Swift (SPEC-0006 RF-8).
 */
@OptIn(ExperimentalForeignApi::class)
class OnnxRunnerImpl(
    private val modelProvider: suspend () -> ByteArray,
) : OnnxRunner {
    private val initMutex = Mutex()
    private var modelPath: String? = null

    override suspend fun run(input: FloatArray): OnnxOutput {
        val runner =
            checkNotNull(OnnxBridge.runner) {
                "El puente nativo de ONNX no está instalado: falta OnnxRuntimeBridge.install() en Swift."
            }
        val result = runner(ensureModelPath(), input.toNSData())
        val output = result.output
        check(result.error == null && output != null) {
            "ONNX Runtime falló en iOS: ${result.error ?: "no devolvió tensor de salida"}"
        }
        return OnnxOutput(data = output.toFloatArray(), attributes = result.attributes)
    }

    private suspend fun ensureModelPath(): String =
        initMutex.withLock {
            modelPath ?: resolveModelPath().also { modelPath = it }
        }

    /**
     * `ORTSession` solo admite una ruta de fichero, así que se resuelve la del `.onnx` **dentro del paquete
     * de la app** (Compose Resources) y se usa tal cual: sin copiar 39 MB.
     *
     * Si esa ruta no existiera en disco —un cambio interno de Compose Resources, o el recurso empaquetado en
     * el bundle del framework en vez de en el de la app— se cae al camino de reserva: extraerlo a la caché
     * una única vez.
     */
    private suspend fun resolveModelPath(): String {
        val bundled = cellDetectorModelUri().toLocalFilePath()
        if (bundled != null && NSFileManager.defaultManager.fileExistsAtPath(bundled)) return bundled
        return extractModelToCaches()
    }

    /** La URI puede venir como ruta suelta o como `file://…` (con escapes), que resuelve `NSURL`. */
    private fun String.toLocalFilePath(): String? =
        if (startsWith(FILE_SCHEME)) NSURL.URLWithString(this)?.path else this

    private suspend fun extractModelToCaches(): String {
        val caches = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).firstOrNull()
        val directory = checkNotNull(caches as? String) { "No se pudo localizar el directorio de caché." }
        val target = "$directory/$MODEL_CACHE_FILE"
        if (!NSFileManager.defaultManager.fileExistsAtPath(target)) {
            // `atomically = true` escribe en un temporal y renombra: el fichero o está completo o no está,
            // así que encontrarlo ya es garantía suficiente para reutilizarlo.
            val written = modelProvider().toNSData().writeToFile(target, atomically = true)
            check(written) { "No se pudo extraer el modelo ONNX a la caché." }
        }
        return target
    }

    private fun ByteArray.toNSData(): NSData =
        usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
        }

    private fun FloatArray.toNSData(): NSData =
        usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = (size * Float.SIZE_BYTES).toULong())
        }

    private fun NSData.toFloatArray(): FloatArray {
        val count = (length.toLong() / Float.SIZE_BYTES).toInt()
        val floats = FloatArray(count)
        if (count > 0) {
            floats.usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
        }
        return floats
    }

    private companion object {
        const val FILE_SCHEME = "file://"
        const val MODEL_CACHE_FILE = "citovision-cell-detector.onnx"
    }
}
