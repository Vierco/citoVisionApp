package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.RemotePatientAnalyses
import dev.lovelace.citovision.core.result.Result
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

class ListPatientCodesUseCaseTest {
    private val authService = mock<AuthService>()
    private val remote = mock<RemotePatientAnalyses>()
    private val useCase = ListPatientCodesUseCase(authService, remote)

    private val account = AuthUser("u1", "a@b.com", null, isGuest = false)

    @Test
    fun `given a guest when listing then requires account`() =
        runTest {
            every { authService.currentUser } returns flowOf(AuthUser("guest", null, null, isGuest = true))

            assertEquals(PatientCodesResult.RequiresAccount, useCase())
        }

    @Test
    fun `given codes for the user when listing then returns them`() =
        runTest {
            every { authService.currentUser } returns flowOf(account)
            everySuspend { remote.queryPatientCodes("u1") } returns Result.Success(listOf("12-34", "20-26"))

            assertEquals(PatientCodesResult.Loaded(listOf("12-34", "20-26")), useCase())
        }

    @Test
    fun `given no analyses when listing then returns an empty list instead of an error`() =
        runTest {
            every { authService.currentUser } returns flowOf(account)
            everySuspend { remote.queryPatientCodes("u1") } returns Result.Success(emptyList())

            assertEquals(PatientCodesResult.Loaded(emptyList()), useCase())
        }

    @Test
    fun `given a network failure when listing then returns error`() =
        runTest {
            every { authService.currentUser } returns flowOf(account)
            everySuspend { remote.queryPatientCodes("u1") } returns Result.Failure(RemoteAnalysisError.Network)

            assertEquals(PatientCodesResult.Error, useCase())
        }
}
