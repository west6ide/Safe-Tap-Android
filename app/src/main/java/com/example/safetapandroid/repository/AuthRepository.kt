package com.example.safetapandroid.repository

import android.content.Context
import android.util.Log
import com.example.safetapandroid.network.*
import com.example.safetapandroid.network.AuthResponse
import com.example.safetapandroid.utils.UserManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.SocketTimeoutException

class AuthRepository(private val context: Context) {  // Передаем context в конструктор
    private val api = RetrofitClient.getInstance(context).create(AuthApi::class.java)


    fun loginUser(phone: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val request = LoginRequest(phone, password)

        api.login(request).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val accessToken = response.body()?.token ?: ""

                    if (accessToken.isNotEmpty()) {
                        UserManager.saveAuthTokens(context, accessToken, "")
                        Log.d("Auth", "Токен успешно сохранён из JSON: $accessToken")
                    } else {
                        UserManager.loadTokenFromCookies(context, response) // ✅ Загружаем токен из Cookie
                    }

                    onResult(true, accessToken)
                } else {
                    onResult(false, null)
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                onResult(false, "Ошибка: ${t.localizedMessage}")
            }
        })
    }


    fun registerUser(name: String, phone: String, password: String, confirmPassword: String, onResult: (Boolean, String?) -> Unit) {
        val request = RegisterRequest(name, phone, password, confirmPassword)

        api.register(request).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    onResult(true, response.body()?.token)
                } else {
                    onResult(false, null)
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                onResult(false, null)
            }
        })
    }


//    fun googleSignIn(idToken: String, onResult: (Boolean, String?) -> Unit) {
//        val request = GoogleSignInRequest(idToken)
//
//        api.googleLogin(request).enqueue(object : Callback<AuthResponse> {
//            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
//                if (response.isSuccessful && response.body() != null) {
//                    onResult(true, response.body()?.token)
//                } else {
//                    onResult(false, "Ошибка авторизации через Google")
//                }
//            }
//
//            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
//                onResult(false, "Ошибка подключения: ${t.localizedMessage}")
//            }
//        })
//    }


}
