package dev.lovelace.citovision.infrastructure.auth

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.AuthUser
import dev.lovelace.citovision.domain.errors.AuthError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementación de [AuthService] con Firebase Auth (GitLive) para Android.
 * Traduce las excepciones de Firebase a [AuthError] (RNF-4). El acceso como invitado es local
 * (no crea cuenta Firebase), coherente con SPEC-0001 RN-3.
 */
class FirebaseAuthService : AuthService {
    private val auth = Firebase.auth

    override val currentUser: Flow<AuthUser?> =
        auth.authStateChanged.map { it?.toAuthUser() }

    override suspend fun signInWithEmail(
        email: String,
        password: String,
    ): Result<AuthUser, AuthError> =
        runCatchingAuth {
            val user =
                auth.signInWithEmailAndPassword(email, password).user
                    ?: return Result.Failure(AuthError.Unknown("null user"))
            Result.Success(user.toAuthUser())
        }

    override suspend fun signInWithGoogle(idToken: String): Result<AuthUser, AuthError> =
        runCatchingAuth {
            val credential = GoogleAuthProvider.credential(idToken = idToken, accessToken = null)
            val user =
                auth.signInWithCredential(credential).user
                    ?: return Result.Failure(AuthError.Unknown("null user"))
            Result.Success(user.toAuthUser())
        }

    override suspend fun signInAsGuest(): Result<AuthUser, AuthError> =
        Result.Success(
            AuthUser(uid = GUEST_UID, email = null, displayName = null, isGuest = true),
        )

    override suspend fun signOut(): Result<Unit, AuthError> =
        runCatchingAuth {
            auth.signOut()
            Result.Success(Unit)
        }

    override suspend fun sendPasswordReset(email: String): Result<Unit, AuthError> =
        runCatchingAuth {
            auth.sendPasswordResetEmail(email)
            Result.Success(Unit)
        }

    private inline fun <T> runCatchingAuth(block: () -> Result<T, AuthError>): Result<T, AuthError> =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.Failure(AuthError.InvalidCredentials)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.Failure(AuthError.UserNotFound)
        } catch (e: Exception) {
            Result.Failure(AuthError.Unknown(e.message))
        }

    private fun FirebaseUser.toAuthUser(): AuthUser =
        AuthUser(uid = uid, email = email, displayName = displayName, isGuest = false, photoUrl = photoURL)

    private companion object {
        const val GUEST_UID = "guest"
    }
}
