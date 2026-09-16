package com.example.sayvis.auth

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

/**
 * ODIN QUANT - Google Authentication Manager
 * Handles Google Sign-In via Credential Manager + Firebase Auth
 * 
 * Flow:
 * 1. User taps "Sign in with Google"
 * 2. Credential Manager shows Google accounts
 * 3. Get ID token
 * 4. Authenticate with Firebase
 * 5. Return FirebaseUser
 * 
 * Also supports:
 * - Check current user
 * - Sign out
 * - Delete account
 * - Link with existing email/password account
 */

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isEmailVerified: Boolean,
    val provider: String // google.com, password, etc.
)

sealed class AuthResult {
    data class Success(val user: AuthUser) : AuthResult()
    data class Error(val messageFa: String, val messageEn: String, val exception: Exception? = null) : AuthResult()
    object Cancelled : AuthResult()
}

class GoogleAuthManager(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    // TODO: Replace with your actual web client ID from google-services.json
    // You can get it from Firebase Console -> Project Settings -> General -> Web client ID
    // Or from google-services.json -> client -> oauth_client with client_type 3
    private val webClientId = run {
        try {
            // Try to get from resources if google-services.json exists
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else "YOUR_WEB_CLIENT_ID"
        } catch (e: Exception) {
            "YOUR_WEB_CLIENT_ID"
        }
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
            Log.d("GoogleAuth", "Starting Google Sign-In with webClientId: ${webClientId.take(20)}...")

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // Show all Google accounts, not just authorized
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false) // Don't auto-select, show picker
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                Log.d("GoogleAuth", "Got ID token, authenticating with Firebase...")

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
                    Log.d("GoogleAuth", "Firebase auth success: ${user.email}")
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error(
                        "ورود با گوگل ناموفق بود - کاربر Firebase null",
                        "Google sign-in failed - Firebase user null"
                    )
                }
            } else {
                AuthResult.Error(
                    "نوع اعتبارنامه نامعتبر است",
                    "Invalid credential type: ${credential.type}"
                )
            }

        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            Log.d("GoogleAuth", "User cancelled Google Sign-In")
            AuthResult.Cancelled
        } catch (e: Exception) {
            Log.e("GoogleAuth", "Google Sign-In error", e)
            AuthResult.Error(
                "خطا در ورود با گوگل: ${e.message}",
                "Google Sign-In error: ${e.message}",
                e
            )
        }
    }

    suspend fun signOut(): AuthResult {
        return try {
            auth.signOut()
            // Also clear credential manager state
            credentialManager.clearCredentialState(
                androidx.credentials.ClearCredentialStateRequest()
            )
            AuthResult.Success(
                AuthUser("", null, null, null, false, "signed_out")
            )
        } catch (e: Exception) {
            AuthResult.Error(
                "خطا در خروج: ${e.message}",
                "Sign out error: ${e.message}",
                e
            )
        }
    }

    suspend fun deleteAccount(): AuthResult {
        return try {
            val user = auth.currentUser
            if (user == null) {
                return AuthResult.Error("کاربری وارد نشده", "No user logged in")
            }
            user.delete().await()
            AuthResult.Success(AuthUser("", null, null, null, false, "deleted"))
        } catch (e: Exception) {
            AuthResult.Error(
                "خطا در حذف حساب: ${e.message}",
                "Delete account error: ${e.message}",
                e
            )
        }
    }

    fun getIdToken(): String? {
        // For backend authentication
        return null // Implement if needed via getIdToken(false).await()
    }
}
