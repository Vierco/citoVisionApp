package dev.lovelace.citovision.infrastructure.inference

import dev.lovelace.citovision.application.ports.CellDetector
import dev.lovelace.citovision.application.ports.ImageDecoder
import dev.lovelace.citovision.application.ports.OnnxRunner
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.Detection
import dev.lovelace.citovision.domain.entities.SelectedImage
import dev.lovelace.citovision.domain.errors.InferenceError
import dev.lovelace.citovision.domain.inference.YoloPostprocessor
import dev.lovelace.citovision.domain.inference.YoloPreprocessor
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * Orquesta la detección celular on-device (SPEC-0006, ADR-0003): decodifica la imagen (por plataforma),
 * la preprocesa, ejecuta el modelo ONNX y postprocesa la salida, todo **fuera del hilo principal** con el
 * [dispatcher] inyectado. La lógica determinista (pre/postprocesado) vive en `commonMain` y es común a las
 * tres plataformas; solo [imageDecoder] y [onnxRunner] son específicos de plataforma.
 */
class CellDetectorImpl(
    private val imageDecoder: ImageDecoder,
    private val onnxRunner: OnnxRunner,
    private val dispatcher: CoroutineDispatcher,
    private val iouThreshold: Float = DEFAULT_IOU_THRESHOLD,
) : CellDetector {
    override suspend fun detect(image: SelectedImage): Result<List<Detection>, InferenceError> =
        withContext(dispatcher) {
            val rgbImage =
                imageDecoder.decode(image.bytes)
                    ?: return@withContext Result.Failure(InferenceError.ImageDecodeFailed)
            try {
                val preprocessed = YoloPreprocessor.preprocess(rgbImage)
                val output = onnxRunner.run(preprocessed.input)
                val detections =
                    YoloPostprocessor.decode(
                        output = output.data,
                        attributes = output.attributes,
                        transform = preprocessed.transform,
                        iouThreshold = iouThreshold,
                    )
                Result.Success(detections)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Napier.w("Fallo de inferencia celular", error)
                Result.Failure(InferenceError.InferenceFailed)
            }
        }

    private companion object {
        const val DEFAULT_IOU_THRESHOLD = 0.45f
    }
}
