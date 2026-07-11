package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.RemoteAnalysisSync
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

class SyncAnalysisUseCaseTest {
    private val authService = mock<AuthService>()
    private val remoteAnalysisSync = mock<RemoteAnalysisSync>()
    private val useCase = SyncAnalysisUseCase(authService, remoteAnalysisSync)

    @Test
    fun `given a guest when syncing then skips remote`() =
        runTest {
            every { authService.currentUser } returns flowOf(AuthUser("guest", null, null, isGuest = true))

            assertEquals(SyncOutcome.SkippedGuest, useCase("a1"))
        }

    @Test
    fun `given no session when syncing then skips remote`() =
        runTest {
            every { authService.currentUser } returns flowOf(null)

            assertEquals(SyncOutcome.SkippedGuest, useCase("a1"))
        }

    @Test
    fun `given an account and a successful push when syncing then returns synced`() =
        runTest {
            every { authService.currentUser } returns flowOf(AuthUser("u1", "a@b.com", null, isGuest = false))
            everySuspend { remoteAnalysisSync.enqueue("a1", "u1") } returns Unit
            everySuspend { remoteAnalysisSync.processPending() } returns Result.Success(Unit)

            assertEquals(SyncOutcome.Synced, useCase("a1"))
        }

    @Test
    fun `given an account and a failing push when syncing then returns failed`() =
        runTest {
            every { authService.currentUser } returns flowOf(AuthUser("u1", "a@b.com", null, isGuest = false))
            everySuspend { remoteAnalysisSync.enqueue("a1", "u1") } returns Unit
            everySuspend { remoteAnalysisSync.processPending() } returns Result.Failure(RemoteAnalysisError.Network)

            assertEquals(SyncOutcome.Failed, useCase("a1"))
        }
}
