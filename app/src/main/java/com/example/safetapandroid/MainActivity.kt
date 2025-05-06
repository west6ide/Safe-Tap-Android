package com.example.safetapandroid

import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import com.example.safetapandroid.repository.AuthRepository
import com.example.safetapandroid.ui.AuthScreen
import com.example.safetapandroid.ui.MapsActivity
import com.example.safetapandroid.viewmodel.AuthViewModel
import com.example.safetapandroid.viewmodel.AuthViewModelFactory
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

class MainActivity : ComponentActivity() {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        // 🔥 Show splash screen immediately
        installSplashScreen()

        super.onCreate(savedInstanceState)

        val authRepository = AuthRepository(application)
        authViewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(application, authRepository)
        )[AuthViewModel::class.java]

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(GOOGLE_CLIENT_ID)
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            if (isUserLoggedIn()) {
                startActivity(Intent(this, MapsActivity::class.java))
                finish()
            } else {
                AuthScreen(
                    viewModel = authViewModel,
                    onGoogleSignInClick = { signInWithGoogle() }
                )
            }
        }
    }

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val idToken = account.idToken
            Log.d("GoogleSignIn", "Успешный вход! ID Token: $idToken")
            authViewModel.authenticateWithGoogle(idToken ?: "")
        } catch (e: ApiException) {
            Log.w("GoogleSignIn", "Ошибка входа: ${e.statusCode}")
        }
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return !sharedPreferences.getString("token", null).isNullOrEmpty()
    }

    companion object {
        private const val GOOGLE_CLIENT_ID =
            "322314763755-fmvoda6hptperhjhvvdov2r1nfjv34r7.apps.googleusercontent.com"
    }
}


