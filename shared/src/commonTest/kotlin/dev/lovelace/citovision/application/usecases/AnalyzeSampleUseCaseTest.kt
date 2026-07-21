package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.AnalysisImageStore
import dev.lovelace.citovision.application.ports.AnalysisRepository
import dev.lovelace.citovision.application.ports.CellDetector
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.Analysis
import dev.lovelace.citovision.domain.entities.BoundingBox
import dev.lovelace.citovision.domain.entities.CellClass
import dev.lovelace.citovision.domain.entities.Detection
import dev.lovelace.citovision.domain.entities.DetectionLevel
import dev.lovelace.citovision.domain.entities.Priority
import dev.lovelace.citovision.domain.entities.SelectedImage
import dev.lovelace.citovision.domain.errors.AnalysisError
import dev.lovelace.citovision.domain.errors.InferenceError
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Sustituto del andamiaje mock (SPEC-0006): inferencia → derivación de conteo/resumen/prioridad → guardado.
 * Cubre RN-7 (sin células no persiste, pero un hallazgo de baja confianza sí basta para guardar), RF-7
 * (fallo de inferencia) y el borrado de imagen huérfana.
 */
class AnalyzeSampleUseCaseTest {
    private val cellDetector = mock<CellDetector>()
    private val analysisRepository = mock<AnalysisRepository>()
    private val analysisImageStore = mock<AnalysisImageStore>()
    private val useCase = AnalyzeSampleUseCase(cellDetector, analysisRepository, analysisImageStore)

    private val image =
        SelectedImage(bytes = ByteArray(4), fileName = "muestra.png", mimeType = "image/png", sizeBytes = 4)

    @Test
    fun `given detections with cells when invoking then it saves and returns Saved`() =
        runTest {
            everySuspend { cellDetector.detect(any()) } returns Result.Success(detectionsOf(CellClass.BLASTO))
            everySuspend { analysisImageStore.save(any(), any()) } returns Result.Success("/tmp/a.png")
            everySuspend { analysisRepository.saveAnalysis(any()) } returns Result.Success(Unit)

            val outcome = useCase(image, "12-34")

            assertTrue(outcome is AnalysisOutcome.Saved)
            verifySuspend { analysisRepository.saveAnalysis(any()) }
        }

    @Test
    fun `given inference fails when invoking then returns InferenceFailed without saving`() =
        runTest {
            everySuspend { cellDetector.detect(any()) } returns Result.Failure(InferenceError.InferenceFailed)

            assertEquals(AnalysisOutcome.InferenceFailed, useCase(image, "12-34"))
        }

    @Test
    fun `given only non-cell detections when invoking then returns NoCellsDetected without saving`() =
        runTest {
            everySuspend { cellDetector.detect(any()) } returns
                Result.Success(detectionsOf(CellClass.ARTEFACTO, CellClass.RESTOS_CELULARES))

            assertEquals(AnalysisOutcome.NoCellsDetected, useCase(image, "12-34"))
        }

    @Test
    fun `given the image cannot be written when invoking then returns SaveFailed`() =
        runTest {
            everySuspend { cellDetector.detect(any()) } returns Result.Success(detectionsOf(CellClass.LINFOCITO))
            everySuspend { analysisImageStore.save(any(), any()) } returns
                Result.Failure(AnalysisError.StorageFailure)

            assertEquals(AnalysisOutcome.SaveFailed, useCase(image, "12-34"))
        }

    @Test
    fun `given the row cannot be inserted when invoking then deletes the orphan image`() =
        runTest {
            everySuspend { cellDetector.detect(any()) } returns Result.Success(detectionsOf(CellClass.LINFOCITO))
            everySuspend { analysisImageStore.save(any(), any()) } returns Result.Success("/tmp/a.png")
            everySuspend { analysisRepository.saveAnalysis(any()) } returns
                Result.Failure(AnalysisError.StorageFailure)
            everySuspend { analysisImageStore.delete("/tmp/a.png") } returns Result.Success(Unit)

            assertEquals(AnalysisOutcome.SaveFailed, useCase(image, "12-34"))
            verifySuspend { analysisImageStore.delete("/tmp/a.png") }
        }

    @Test
    fun `given a saved analysis when invoking then it carries priority and counts`() =
        runTest {
            val capturingRepository = CapturingAnalysisRepository()
            everySuspend { cellDetector.detect(any()) } returns
                Result.Success(detectionsOf(CellClass.BLASTO, CellClass.LINFOCITO))
            everySuspend { analysisImageStore.save(any(), any()) } returns Result.Success("/tmp/a.png")

            AnalyzeSampleUseCase(cellDetector, capturingRepository, analysisImageStore).invoke(image, "12-34")

            val persisted = requireNotNull(capturingRepository.saved)
            assertEquals(Priority.ALTA, persisted.priority)
            assertEquals("12-34", persisted.patient)
            assertEquals("/tmp/a.png", persisted.imagePath)
            // El nombre de la muestra es el del fichero sin extensión ("muestra.png" → "muestra").
            assertEquals("muestra", persisted.sampleName)
            assertTrue(persisted.cellCounts.isNotEmpty())
        }

    @Test
    fun `given only low confidence findings when invoking then it still saves the analysis`() =
        runTest {
            // RN-7: perder el indicio por no tener nada que contar sería el peor resultado posible.
            val capturingRepository = CapturingAnalysisRepository()
            everySuspend { cellDetector.detect(any()) } returns
                Result.Success(listOf(reviewDetection(CellClass.BLASTO)))
            everySuspend { analysisImageStore.save(any(), any()) } returns Result.Success("/tmp/a.png")

            val outcome =
                AnalyzeSampleUseCase(cellDetector, capturingRepository, analysisImageStore)
                    .invoke(image, "12-34")

            assertTrue(outcome is AnalysisOutcome.Saved)
            val persisted = requireNotNull(capturingRepository.saved)
            // Cero células contadas, pero el hallazgo sube la prioridad a MEDIA (RN-9b) y queda registrado.
            assertEquals(Priority.MEDIA, persisted.priority)
            assertTrue(persisted.summary.startsWith("0 células detectadas."))
            assertTrue(persisted.summary.contains("Posibles hallazgos de baja confianza"))
            assertEquals(
                listOf(DetectionLevel.LOW_CONFIDENCE_REVIEW),
                persisted.cellCounts.map { it.level },
            )
        }

    private fun detectionsOf(vararg classes: CellClass): List<Detection> =
        classes.map { Detection(cellClass = it, confidence = 0.9f, box = BoundingBox(0f, 0f, 1f, 1f)) }

    private fun reviewDetection(cellClass: CellClass): Detection =
        Detection(
            cellClass = cellClass,
            confidence = 0.09f,
            box = BoundingBox(0f, 0f, 1f, 1f),
            level = DetectionLevel.LOW_CONFIDENCE_REVIEW,
        )

    private class CapturingAnalysisRepository : AnalysisRepository {
        var saved: Analysis? = null

        override fun observeAnalyses(): Flow<List<Analysis>> = flowOf(emptyList())

        override suspend fun saveAnalysis(analysis: Analysis): Result<Unit, AnalysisError> {
            saved = analysis
            return Result.Success(Unit)
        }

        override suspend fun deleteAnalysis(id: String): Result<Unit, AnalysisError> = Result.Success(Unit)

        override suspend fun deleteAllAnalyses(): Result<Unit, AnalysisError> = Result.Success(Unit)
    }
}
