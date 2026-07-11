package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.RemotePatientAnalyses
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.Analysis
import dev.lovelace.citovision.domain.entities.AuthUser
import dev.lovelace.citovision.domain.errors.RemoteAnalysisError
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class SearchPatientAnalysesUseCaseTest {
    private val authService = mock<AuthService>()
    private val remote = mock<RemotePatientAnalyses>()
    private val useCase = SearchPatientAnalysesUseCase(authService, remote)

    private val account = AuthUser("u1", "a@b.com", null, isGuest = false)

    private fun analysis(id: String) = Analysis(id, "12-34", Instant.fromEpochMilliseconds(1), "s", null, emptyList())

    @Test
    fun `given a guest when searching then requires account`() =
        runTest {
            every { authService.currentUser } returns flowOf(AuthUser("guest", null, null, isGuest = true))

            assertEquals(PatientSearchResult.RequiresAccount, useCase("12-34"))
        }

    @Test
    fun `given results when searching then returns found`() =
        runTest {
            every { authService.currentUser } returns flowOf(account)
            everySuspend { remote.queryByPatient("u1", "12-34") } returns Result.Success(listOf(analysis("a1")))

            val result = useCase("12-34")

            assertTrue(result is PatientSearchResult.Found)
            assertEquals(1, result.analyses.size)
        }

    @Test
    fun `given no results when searching then returns empty`() =
        runTest {
            every { authService.currentUser } returns flowOf(account)
            everySuspend { remote.queryByPatient("u1", "12-34") } returns Result.Success(emptyList())

            assertEquals(PatientSearchResult.Empty, useCase("12-34"))
        }

    @Test
    fun `given a network failure when searching then returns error`() =
        runTest {
            every { authService.currentUser } returns flowOf(account)
            everySuspend { remote.queryByPatient("u1", "12-34") } returns Result.Failure(RemoteAnalysisError.Network)

            assertEquals(PatientSearchResult.Error, useCase("12-34"))
        }
}
