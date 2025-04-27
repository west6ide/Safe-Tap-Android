package com.example.safetapandroid.network
//
//import android.app.Activity
//import android.content.Context
//import android.util.Log
//import androidx.credentials.CredentialManager
//import androidx.credentials.GetCredentialRequest
//import androidx.credentials.GetCredentialResponse
//import androidx.credentials.exceptions.GetCredentialException
//import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//
//class GoogleSignInHelper(private val context: Context) {
//    private val credentialManager = CredentialManager.create(context)
//
//    fun signInWithGoogle(activity: Activity, onResult: (Boolean, String?) -> Unit) {
//        CoroutineScope(Dispatchers.Main).launch {
//            try {
//                val googleIdOption = GetSignInWithGoogleOption.Builder()
//                    .setServerClientId(WEB_CLIENT_ID) // ✅ Используем константу WEB_CLIENT_ID
//                    .setNonce(generateNonce()) // ✅ Генерируем уникальный nonce
//                    .build()
//
//                val request = GetCredentialRequest.Builder()
//                    .addCredentialOption(googleIdOption)
//                    .build()
//
//                val result: GetCredentialResponse = credentialManager.getCredential(
//                    request = request,
//                    context = activity
//                )
//
//                handleSignInResult(result, onResult)
//
//            } catch (e: GetCredentialException) {
//                Log.e("GoogleSignInHelper", "Ошибка входа: ${e.message}")
//                onResult(false, "Ошибка входа: ${e.localizedMessage}")
//            }
//        }
//    }
//
//    private fun handleSignInResult(result: GetCredentialResponse, onResult: (Boolean, String?) -> Unit) {
//        val credential = result.credential
//
//        try {
//            val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
//            val idToken = googleIdTokenCredential.idToken
//
//            if (!idToken.isNullOrEmpty()) {
//                onResult(true, idToken)
//            } else {
//                onResult(false, "Ошибка получения токена")
//            }
//        } catch (e: Exception) {
//            onResult(false, "Ошибка обработки Google ID Token: ${e.localizedMessage}")
//        }
//    }
//
//    // ✅ Функция для генерации случайного nonce
//    private fun generateNonce(): String {
//        return System.currentTimeMillis().toString() // Можно заменить на более сложную генерацию
//    }
//
//    companion object {
//        private const val WEB_CLIENT_ID = "322314763755-fmvoda6hptperhjhvvdov2r1nfjv34r7.apps.googleusercontent.com" // ✅ Укажите CLIENT_ID из Google Console
//    }
//}
