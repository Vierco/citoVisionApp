package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.RemoteFeedback
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.AuthUser
import dev.lovelace.citovision.domain.entities.Feedback
import dev.lovelace.citovision.domain.errors.RemoteFeedbackError
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubmitFeedbackUseCaseTest {
    private val authService = mock<AuthService>()

    /** Fake que captura el feedback recibido y devuelve un resultado configurable. */
    private class RecordingFeedback(
        private val result: Result<Unit, RemoteFeedbackError>,
    ) : RemoteFeedback {
        var last: Feedback? = null

        override suspend fun submit(feedback: Feedback): Result<Unit, RemoteFeedbackError> {
            last = feedback
            return result
        }
    }

    @Test
    fun `given an account when submitting then attaches ownerUid and trims fields`() =
        runTest {
            every { authService.currentUser } returns flowOf(AuthUser("u1", "a@b.com", null, isGuest = false))
            val remote = RecordingFeedback(Result.Success(Unit))
            val useCase = SubmitFeedbackUseCase(remote, authService)

            assertEquals(FeedbackResult.SENT, useCase(email = "  a@b.com  ", message = "  Hola  "))
            assertEquals("u1", remote.last?.ownerUid)
            assertEquals("a@b.com", remote.last?.email)
            assertEquals("Hola", remote.last?.message)
        }

    @Test
    fun `given a guest when submitting then requires an account and skips the network`() =
        runTest {
            every { authService.currentUser } returns flowOf(AuthUser("guest", null, null, isGuest = true))
            val remote = RecordingFeedback(Result.Success(Unit))
            val useCase = SubmitFeedbackUseCase(remote, authService)

            assertEquals(FeedbackResult.REQUIRES_ACCOUNT, useCase(email = "a@b.com", message = "Hola"))
            assertNull(remote.last)
        }

    @Test
    fun `given no session when submitting then requires an account`() =
        runTest {
            every { authService.currentUser } returns flowOf(null)
            val remote = RecordingFeedback(Result.Success(Unit))
            val useCase = SubmitFeedbackUseCase(remote, authService)

            assertEquals(FeedbackResult.REQUIRES_ACCOUNT, useCase(email = "a@b.com", message = "Hola"))
            assertNull(remote.last)
        }

    @Test
    fun `given a remote failure when submitting then returns an error`() =
        runTest {
            every { authService.currentUser } returns flowOf(AuthUser("u1", "a@b.com", null, isGuest = false))
            val remote = RecordingFeedback(Result.Failure(RemoteFeedbackError.Network))
            val useCase = SubmitFeedbackUseCase(remote, authService)

            assertEquals(FeedbackResult.ERROR, useCase(email = "a@b.com", message = "Hola"))
        }
}
