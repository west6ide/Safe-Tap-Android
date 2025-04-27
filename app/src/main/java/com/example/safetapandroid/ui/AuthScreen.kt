package com.example.safetapandroid.ui

import android.app.Activity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.safetapandroid.viewmodel.AuthViewModel
import android.content.Intent

@Composable
fun AuthScreen(viewModel: AuthViewModel, onGoogleSignInClick: () -> Unit) {

    val context = LocalContext.current
    val isAuthenticated by viewModel.isAuthenticated.collectAsState(initial = viewModel.isUserLoggedIn())

    if (isAuthenticated) {
        LaunchedEffect(Unit) {
            val intent = Intent(context, MapsActivity::class.java)
            context.startActivity(intent)
            (context as? Activity)?.finish()
        }
    } else {
        var isSignUp by remember { mutableStateOf(false) }
        if (isSignUp) {
            SignUpScreen(
                viewModel = viewModel,
                onSignInClick = { isSignUp = false },
                onSuccessRegister = { viewModel }
            )
        } else {
            SignInScreen(
                viewModel = viewModel,
                onSignUpClick = { isSignUp = true },
                onLoginSuccess = { viewModel },
                onGoogleSignInClick
            )
        }
    }
}
