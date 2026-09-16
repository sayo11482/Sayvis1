package com.odin.agent.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isEmailVerified: Boolean,
    val provider: String
)

sealed class AuthResult {
    data class Success(val user: AuthUser) : AuthResult()
    data class Error(val messageFa: String, val messageEn: String, val exception: Exception? = null) : AuthResult()
    object Cancelled : AuthResult()
}

class GoogleAuthManager(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    private val webClientId = try {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) context.getString(resId) else "YOUR_WEB_CLIENT_ID"
    } catch (e: Exception) {
        "YOUR_WEB_CLIENT_ID"
    }

    fun getCurrentUser(): AuthUser? {
        val firebaseUser = auth.currentUser ?: return null
        return AuthUser(
            uid = firebaseUser.uid,
            email = firebaseUser.email,
            displayName = firebaseUser.displayName,
            photoUrl = firebaseUser.photoUrl?.toString(),
            isEmailVerified = firebaseUser.isEmailVerified,
            provider = firebaseUser.providerData.firstOrNull()?.providerId ?: "unknown"
        )
    }

    fun isLoggedIn(): Boolean = auth.currentUser != null

    suspend fun signInWithGoogle(): AuthResult {
        return try {
            Log.d("OdinAuth", "Starting Google Sign-In")

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(request, context)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                    val user = AuthUser(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email,
                        displayName = firebaseUser.displayName,
                        photoUrl = firebaseUser.photoUrl?.toString(),
                        isEmailVerified = firebaseUser.isEmailVerified,
                        provider = "google.com"
                    )
                    Log.d("OdinAuth", "Auth success: ${user.email}")
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error("ورود ناموفق - کاربر null", "Sign-in failed - user null")
                }
            } else {
                AuthResult.Error("نوع اعتبارنامه نامعتبر", "Invalid credential type")
            }

        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            Log.d("OdinAuth", "Cancelled")
            AuthResult.Cancelled
        } catch (e: Exception) {
            Log.e("OdinAuth", "Error", e)
            AuthResult.Error("خطا: ${e.message}", "Error: ${e.message}", e)
        }
    }

    suspend fun signOut(): AuthResult {
        return try {
            auth.signOut()
            credentialManager.clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
            AuthResult.Success(AuthUser("", null, null, null, false, "signed_out"))
        } catch (e: Exception) {
            AuthResult.Error("خطا در خروج: ${e.message}", "Sign out error: ${e.message}", e)
        }
    }
}
