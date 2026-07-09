package dev.lovelace.citovision.infrastructure.auth

import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dev.lovelace.citovision.application.ports.GoogleSignInLauncher
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.errors.AuthError
import dev.lovelace.citovision.infrastructure.platform.ActivityProvider
import kotlinx.coroutines.CancellationException

/** Clave de propiedad Koin con el Web client ID (server) de Firebase, aportado por la Application. */
const val GOOGLE_WEB_CLIENT_ID_PROPERTY = "google_web_client_id"

/**
 * Obtiene un `idToken` de Google mediante Credential Manager (API recomendada; `GoogleSignInClient`
 * está deprecada). Requiere el Web client ID (server) de Firebase y una Activity visible.
 * Traduce cancelación y fallos a [AuthError] (SPEC-0001).
 */
class AndroidGoogleSignInLauncher(
    private val activityProvider: ActivityProvider,
    private val webClientId: String,
) : GoogleSignInLauncher {
    override suspend fun requestIdToken(): Result<String, AuthError> {
        val activity =
            activityProvider.current
                ?: return Result.Failure(AuthError.GoogleSignInFailed)

        val googleIdOption =
            GetGoogleIdOption
                .Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()
        val request =
            GetCredentialRequest
                .Builder()
                .addCredentialOption(googleIdOption)
                .build()

        return try {
            val response = CredentialManager.create(activity).getCredential(activity, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                Result.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                Result.Failure(AuthError.GoogleSignInFailed)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Result.Failure(AuthError.GoogleSignInCancelled)
        } catch (e: GetCredentialException) {
            Result.Failure(AuthError.GoogleSignInFailed)
        }
    }
}
