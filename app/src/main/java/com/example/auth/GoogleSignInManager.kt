package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object GoogleSignInManager {
    private const val TAG = "GoogleSignIn"

    fun getWebClientId(context: Context): String? {
        return try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving default_web_client_id", e)
            null
        }
    }

    /**
     * Silent One-Tap / Auto Sign-In on app startup.
     * Searches for previously authorized accounts with zero user friction.
     */
    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onTokenReceived: (idToken: String) -> Unit,
        onNoCredential: () -> Unit
    ) {
        val clientId = getWebClientId(context)
        if (clientId.isNullOrBlank()) {
            onNoCredential()
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    onTokenReceived(googleIdToken)
                } else {
                    onNoCredential()
                }
            } catch (e: NoCredentialException) {
                Log.d(TAG, "No cached credentials for auto sign-in")
                onNoCredential()
            } catch (e: Exception) {
                Log.d(TAG, "Auto sign-in bypassed: ${e.message}")
                onNoCredential()
            }
        }
    }

    /**
     * Interactive One-Tap Google Sign-In clicked by user.
     * Opens Android native bottom sheet showing accounts active on the device.
     */
    fun launchInteractiveGoogleSignIn(
        activity: Activity,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onTokenReceived: (idToken: String) -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit
    ) {
        val clientId = getWebClientId(activity)
        if (clientId.isNullOrBlank()) {
            onError("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    onTokenReceived(googleIdToken)
                } else {
                    onError("Unexpected credential type received")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled: ${e.message}", e)
                onCancelled()
            } catch (e: NoCredentialException) {
                Log.w(TAG, "No Google accounts available on device: ${e.message}")
                onError("તમારા ફોનમાં સક્રિય Google એકાઉન્ટ મળ્યું નથી.")
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In error", e)
                val msg = e.localizedMessage ?: "Google Sign-In failed"
                if (msg.contains("16:") || msg.contains("cancel", ignoreCase = true)) {
                    onCancelled()
                } else if (msg.contains("10:") || msg.contains("Developer", ignoreCase = true)) {
                    onError("Google Sign-In [Developer Error 10]: Firebase Console માં આ એપનું SHA-1 ફિંગરપ્રિન્ટ રજીસ્ટર કરવું જરૂરી છે. તમે નીચે આપેલા 'ઝડપી 1-ક્લિક સાઇન ઇન' વડે સીધું ચાલુ કરી શકો છો.")
                } else {
                    onError("Google Sign-In એરર: $msg")
                }
            }
        }
    }

    /**
     * Clears Credential Manager local credential state.
     */
    fun clearCredentials(
        context: Context,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onComplete: () -> Unit = {}
    ) {
        scope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear credential state", e)
            } finally {
                onComplete()
            }
        }
    }
}
