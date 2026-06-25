package com.aistudio.rkaiassistant.data

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object GoogleAuthHelper {
    private const val TAG = "GoogleAuthHelper"
    
    // IMPORTANT: User must provide Web Client ID from Cloud Console
    var serverClientId: String = ""

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail = _userEmail.asStateFlow()

    private val _userName = MutableStateFlow<String?>(null)
    val userName = _userName.asStateFlow()

    suspend fun signIn(context: Context): Boolean {
        if (serverClientId.isEmpty()) {
            Log.e(TAG, "Server Client ID is not set!")
            return false
        }

        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            handleSignInResult(result)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Sign-in failed: ${e.message}")
            false
        }
    }

    private fun handleSignInResult(result: GetCredentialResponse): Boolean {
        val credential = result.credential
        if (credential is GoogleIdTokenCredential) {
            _userEmail.value = credential.id
            _userName.value = credential.displayName
            Log.d(TAG, "User signed in: ${credential.id}")
            return true
        }
        return false
    }

    suspend fun signOut(context: Context) {
        val credentialManager = CredentialManager.create(context)
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
        _userEmail.value = null
        _userName.value = null
    }
}
