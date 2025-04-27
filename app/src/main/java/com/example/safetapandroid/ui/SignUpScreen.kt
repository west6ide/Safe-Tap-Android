package com.example.safetapandroid.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safetapandroid.R
import com.example.safetapandroid.viewmodel.AuthViewModel

@Composable
fun SignUpScreen(viewModel: AuthViewModel, onSignInClick: () -> Unit, onSuccessRegister: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        Image(painter = painterResource(id = R.drawable.sos_logo), contentDescription = "App Logo")
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
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
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        PhoneNumberField(
            phone = phone,
            onValueChange = { phone = it }
        )
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (password != confirmPassword) {
                    errorMessage = "Passwords do not match!"
                    return@Button
                }
                viewModel.registerUser(name, phone, password, confirmPassword) { success ->
                    if (success) onSuccessRegister()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E8B83)),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Sign Up", fontSize = 18.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("or you can sign in with")
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center, // Центрируем иконки
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* TODO: Google SignIn */ }) {
                Image(
                    painter = painterResource(id = R.drawable.google_icon),
                    contentDescription = "Google",
                    modifier = Modifier.size(40.dp) // Можно уменьшить размер иконок
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


        Spacer(modifier = Modifier.height(20.dp))
        TextButton(onClick = onSignInClick) { Text("Have an account? Sign In") }
    }
}

@Composable
fun PhoneNumberField(phone: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = phone,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.kaz_flag),
                    contentDescription = "Kazakhstan Flag",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("+7", fontSize = 16.sp)
            }
        },
        label = { Text("Phone Number") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true
    )
}

