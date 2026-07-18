package dev.lovelace.citovision.infrastructure.inference

import dev.lovelace.citovision.application.ports.ImageDecoder
import dev.lovelace.citovision.application.ports.OnnxOutput
import dev.lovelace.citovision.application.ports.OnnxRunner
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.CellClass
import dev.lovelace.citovision.domain.entities.SelectedImage
import dev.lovelace.citovision.domain.errors.InferenceError
import dev.lovelace.citovision.domain.inference.RgbImage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifica la **orquestación** (SPEC-0006) con puertos falsos: decodificado → preprocesado → ejecución →
 * postprocesado → traducción a [Result]. El motor real y el decodificado nativo se validan en plataforma.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CellDetectorImplTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val image =
        SelectedImage(bytes = ByteArray(0), fileName = "a.png", mimeType = "image/png", sizeBytes = 0)

    @Test
    fun `given an undecodable image when detecting then it fails with ImageDecodeFailed`() =
        runTest {
            val detector = CellDetectorImpl(FakeImageDecoder(null), FakeOnnxRunner(), dispatcher)

            val result = detector.detect(image)

            assertEquals(Result.Failure(InferenceError.ImageDecodeFailed), result)
        }

    @Test
    fun `given a decodable image and model output when detecting then it returns the detections`() =
        runTest {
            val detector =
                CellDetectorImpl(FakeImageDecoder(smallImage()), FakeOnnxRunner(output = blastOutput()), dispatcher)

            val detections =
                when (val result = detector.detect(image)) {
                    is Result.Success -> result.value
                    is Result.Failure -> error("Se esperaba éxito pero falló con ${result.error}")
                }

            assertEquals(1, detections.size)
            assertEquals(CellClass.BLASTO, detections.first().cellClass)
        }

    @Test
    fun `given the runner throws when detecting then it fails with InferenceFailed`() =
        runTest {
            val detector =
                CellDetectorImpl(
                    FakeImageDecoder(smallImage()),
                    FakeOnnxRunner(error = IllegalStateException("boom")),
                    dispatcher,
                )

            val result = detector.detect(image)

            assertEquals(Result.Failure(InferenceError.InferenceFailed), result)
        }

    private fun smallImage(): RgbImage = RgbImage(width = 10, height = 10, pixels = IntArray(100) { 0xFFFFFF })

    private fun blastOutput(): OnnxOutput {
        val output = FloatArray(ATTRIBUTES)
        output[0] = 320f
        output[1] = 320f
        output[2] = 64f
        output[3] = 64f
        output[BOX_CHANNELS + CellClass.BLASTO.index] = 0.9f
        return OnnxOutput(data = output, attributes = ATTRIBUTES)
    }

    private class FakeImageDecoder(
        private val image: RgbImage?,
    ) : ImageDecoder {
        override fun decode(bytes: ByteArray): RgbImage? = image
    }

    private class FakeOnnxRunner(
        private val output: OnnxOutput? = null,
        private val error: Throwable? = null,
    ) : OnnxRunner {
        override suspend fun run(input: FloatArray): OnnxOutput {
            error?.let { throw it }
            return checkNotNull(output) { "FakeOnnxRunner sin salida configurada" }
        }
    }

    private companion object {
        const val BOX_CHANNELS = 4
        const val ATTRIBUTES = BOX_CHANNELS + 14
    }
}
