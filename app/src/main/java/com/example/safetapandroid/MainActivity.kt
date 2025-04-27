package com.example.safetapandroid


import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.safetapandroid.repository.AuthRepository
import com.example.safetapandroid.ui.AuthScreen
import com.example.safetapandroid.ui.SignInScreen
import com.example.safetapandroid.ui.SignUpScreen
import com.example.safetapandroid.viewmodel.AuthViewModel
import com.example.safetapandroid.viewmodel.AuthViewModelFactory
import com.google.android.gms.tasks.Task
import android.util.Log
import android.widget.ImageButton
import com.example.safetapandroid.ui.MapsActivity
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException


class MainActivity : ComponentActivity() {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authRepository = AuthRepository(application)

        authViewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(application, authRepository)
        )[AuthViewModel::class.java]

        // ✅ Настроим Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(GOOGLE_CLIENT_ID) // ✅ Укажите ваш CLIENT_ID
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        if (isUserLoggedIn()) {
            startActivity(Intent(this, MapsActivity::class.java))
            finish()
        } else {
            setContent {
                AuthScreen(
                    viewModel = authViewModel,
                    onGoogleSignInClick = { signInWithGoogle() }
                )
            }
        }
    }
    // ✅ Запуск Google Sign-In
    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    // ✅ Обработчик результата Google Sign-In
    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val idToken = account.idToken
            Log.d("GoogleSignIn", "Успешный вход! ID Token: $idToken")

            // ✅ Передаем idToken в ViewModel для обновления UI
            authViewModel.authenticateWithGoogle(idToken ?: "")

        } catch (e: ApiException) {
            Log.w("GoogleSignIn", "Ошибка входа: ${e.statusCode}")
        }
    }

    companion object {
        private const val GOOGLE_CLIENT_ID = "322314763755-fmvoda6hptperhjhvvdov2r1nfjv34r7.apps.googleusercontent.com" // ✅ Укажите свой CLIENT_ID
    }
    private fun isUserLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)
        return !token.isNullOrEmpty() // ✅ Если токен есть, возвращаем true
    }
}

