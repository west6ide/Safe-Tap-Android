package com.example.safetapandroid.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safetapandroid.MainActivity
import com.example.safetapandroid.R
import com.example.safetapandroid.viewmodel.AuthViewModel

@Composable
fun SignInScreen(viewModel: AuthViewModel, onSignUpClick: () -> Unit, onLoginSuccess: () -> Unit, onGoogleSignInClick: () -> Unit) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val activity = context as? MainActivity // ✅ Исправлено

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        Image(painter = painterResource(id = R.drawable.sos_logo), contentDescription = "App Logo")
        Spacer(modifier = Modifier.height(20.dp))

        PhoneNumberField(
            phone = phone,
            onValueChange = { phone = it }
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                viewModel.loginUser(phone, password) { success ->
                    if (success) onLoginSuccess() else errorMessage = "Invalid credentials"
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E8B83)),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Sign In", fontSize = 18.sp, color = Color.White)
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(10.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(15.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = { /* TODO: Forgot Password Logic */ }) {
                Text("Forgot Password?")
            }
            TextButton(onClick = onSignUpClick) {
                Text("Sign Up")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("or you can sign in with")

        Spacer(modifier = Modifier.height(10.dp))


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center, // Центрируем иконки
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onGoogleSignInClick) { // ✅ Теперь не вызовет ошибку
                Image(
                    painter = painterResource(id = R.drawable.google_icon),
                    contentDescription = "Google Sign-In",
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp)) // Уменьшаем промежуток

            IconButton(onClick = { /* TODO: Facebook SignIn */ }) {
                Image(
                    painter = painterResource(id = R.drawable.facebook_icon),
                    contentDescription = "Facebook",
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp)) // Уменьшаем промежуток

            IconButton(onClick = { /* TODO: Apple SignIn */ }) {
                Image(
                    painter = painterResource(id = R.drawable.apple_icon),
                    contentDescription = "Apple",
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}


