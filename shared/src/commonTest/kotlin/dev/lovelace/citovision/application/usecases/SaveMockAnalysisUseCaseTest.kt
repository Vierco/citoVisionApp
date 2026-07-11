package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.AnalysisImageStore
import dev.lovelace.citovision.application.ports.AnalysisRepository
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.Analysis
import dev.lovelace.citovision.domain.entities.SelectedImage
import dev.lovelace.citovision.domain.errors.AnalysisError
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
 * Andamiaje temporal (SPEC-0004 RF-7): guarda la imagen como fichero y luego la fila. Si la fila falla, el
 * fichero ya escrito debe borrarse para no dejar imágenes huérfanas.
 */
class SaveMockAnalysisUseCaseTest {
    private val analysisRepository = mock<AnalysisRepository>()
    private val analysisImageStore = mock<AnalysisImageStore>()
    private val useCase = SaveMockAnalysisUseCase(analysisRepository, analysisImageStore)

    private val image =
        SelectedImage(bytes = ByteArray(4), fileName = "muestra.png", mimeType = "image/png", sizeBytes = 4)

    @Test
    fun `given the image and row are stored when invoking then returns success`() =
        runTest {
            // Given
            everySuspend { analysisImageStore.save(any(), any()) } returns Result.Success("/tmp/a.png")
            everySuspend { analysisRepository.saveAnalysis(any()) } returns Result.Success(Unit)

            // When
            val result = useCase(image, "12-34")

            // Then
            assertTrue(result is Result.Success)
            verifySuspend { analysisRepository.saveAnalysis(any()) }
        }

    @Test
    fun `given the image cannot be written when invoking then does not save the row`() =
        runTest {
            // Given
            everySuspend { analysisImageStore.save(any(), any()) } returns
                Result.Failure(AnalysisError.StorageFailure)

            // When
            val result = useCase(image, "12-34")

            // Then
            assertEquals(Result.Failure(AnalysisError.StorageFailure), result)
        }

    @Test
    fun `given the row cannot be inserted when invoking then deletes the orphan image`() =
        runTest {
            // Given
            everySuspend { analysisImageStore.save(any(), any()) } returns Result.Success("/tmp/a.png")
            everySuspend { analysisRepository.saveAnalysis(any()) } returns
                Result.Failure(AnalysisError.StorageFailure)
            everySuspend { analysisImageStore.delete("/tmp/a.png") } returns Result.Success(Unit)

            // When
            val result = useCase(image, "12-34")

            // Then
            assertEquals(Result.Failure(AnalysisError.StorageFailure), result)
            verifySuspend { analysisImageStore.delete("/tmp/a.png") }
        }

    @Test
    fun `given a saved analysis when invoking then it carries the image path and a cell count`() =
        runTest {
            // Given: un fake que captura el análisis persistido (más simple que un matcher de captura)
            val capturingRepository = CapturingAnalysisRepository()
            everySuspend { analysisImageStore.save(any(), any()) } returns Result.Success("/tmp/a.png")

            // When
            SaveMockAnalysisUseCase(capturingRepository, analysisImageStore).invoke(image, "12-34")

            // Then
            val persisted = requireNotNull(capturingRepository.saved)
            assertEquals("/tmp/a.png", persisted.imagePath)
            assertTrue(persisted.cellCounts.isNotEmpty())
            assertEquals("12-34", persisted.patient)
        }

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
