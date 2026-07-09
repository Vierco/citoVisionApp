package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.AnalysisRepository
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.errors.AnalysisError
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * El borrado es idempotente (SPEC-0004): que el análisis ya no exista significa que el estado final es el
 * deseado, así que `NotFound` se traduce a éxito. El resto de fallos se propagan.
 */
class DeleteAnalysisUseCaseTest {
    private val analysisRepository = mock<AnalysisRepository>()
    private val useCase = DeleteAnalysisUseCase(analysisRepository)

    @Test
    fun `given an existing analysis when deleting then returns success`() =
        runTest {
            // Given
            everySuspend { analysisRepository.deleteAnalysis("1") } returns Result.Success(Unit)

            // When
            val result = useCase("1")

            // Then
            assertEquals(Result.Success(Unit), result)
            verifySuspend { analysisRepository.deleteAnalysis("1") }
        }

    @Test
    fun `given the analysis no longer exists when deleting then treats it as success`() =
        runTest {
            // Given
            everySuspend { analysisRepository.deleteAnalysis("1") } returns Result.Failure(AnalysisError.NotFound)

            // When
            val result = useCase("1")

            // Then
            assertEquals(Result.Success(Unit), result)
        }

    @Test
    fun `given storage fails when deleting then propagates the failure`() =
        runTest {
            // Given
            everySuspend { analysisRepository.deleteAnalysis("1") } returns
                Result.Failure(AnalysisError.StorageFailure)

            // When
            val result = useCase("1")

            // Then
            assertEquals(Result.Failure(AnalysisError.StorageFailure), result)
        }
}
