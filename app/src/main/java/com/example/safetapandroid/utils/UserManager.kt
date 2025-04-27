package com.example.safetapandroid.utils

import android.content.Context
import android.util.Log
import com.example.safetapandroid.network.AuthApi
import com.example.safetapandroid.network.AuthResponse
import com.example.safetapandroid.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object UserManager {
    private const val TAG = "UserManager"

    fun getUserId(context: Context, callback: (Int?) -> Unit) {
        val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1)

        if (userId != -1) { // Если user_id сохранён, возвращаем его.
            callback(userId)
            return
        }

        fetchUserIdFromServer(context, callback)
    }

    private fun fetchUserIdFromServer(context: Context, callback: (Int?) -> Unit) {
        val token = getAuthToken(context)
        if (token.isNullOrEmpty()) {
            Log.e(TAG, "Ошибка: Токен отсутствует!")
            callback(null)
            return
        }

        val api = RetrofitClient.getInstance(context).create(AuthApi::class.java)
        api.getUserId("Bearer $token").enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.code() == 401) { // Токен просрочен
                    Log.e(TAG, "Токен просрочен! Запрашиваем новый...")
                    refreshToken(context) { newToken ->
                        if (newToken != null) {
                            fetchUserIdFromServer(context, callback)
                        } else {
                            callback(null)
                        }
                    }
                    return
                }

                val responseBody = response.body()
                if (responseBody != null) {
                    val userId = responseBody.user_id?.toInt() // ✅ Преобразуем user_id в Int
                    if (userId != null) {
                        saveUserId(context, userId)
                        callback(userId)
                    } else {
                        callback(null)
                    }
                } else {
                    callback(null)
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                Log.e(TAG, "Ошибка получения UserID: ${t.message}")
                callback(null)
            }
        })
    }

    private fun refreshToken(context: Context, callback: (String?) -> Unit) { // Исправление ошибки refreshToken
        val refreshToken = getRefreshToken(context)
        if (refreshToken.isNullOrEmpty()) {
            Log.e(TAG, "Ошибка: Refresh-токен отсутствует!")
            callback(null)
            return
        }

        val api = RetrofitClient.getInstance(context).create(AuthApi::class.java)
        api.refreshToken("Bearer $refreshToken").enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                val newToken = response.body()?.token ?: ""
                if (newToken.isNotEmpty()) {
                    saveAuthTokens(context, newToken, refreshToken)
                    callback(newToken)
                } else {
                    callback(null)
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                Log.e(TAG, "Ошибка обновления токена: ${t.message}")
                callback(null)
            }
        })
    }

    fun saveAuthTokens(context: Context, accessToken: String, refreshToken: String) {
        val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("auth_token", accessToken)
            putString("refresh_token", refreshToken)
            apply()
        }
        Log.d(TAG, "Токены успешно сохранены")
    }

    fun saveUserId(context: Context, userId: Int) {
        val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putInt("user_id", userId)  // Сохраняем user_id как Int
            apply()
        }
        Log.d(TAG, "UserID успешно сохранён: $userId")
    }

    fun getAuthToken(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("auth_token", null)
    }

    fun getRefreshToken(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("refresh_token", null)
    }

    fun loadTokenFromCookies(context: Context, response: Response<AuthResponse>) {
        val cookies: List<String> = response.headers().values("Set-Cookie")
        var accessToken: String? = null
        var refreshToken: String? = null

        for (cookie in cookies) {
            if (cookie.startsWith("token=")) {
                accessToken = cookie.substringAfter("token=").substringBefore(";")
            }
            if (cookie.startsWith("refresh_token=")) {
                refreshToken = cookie.substringAfter("refresh_token=").substringBefore(";")
            }
        }

        if (!accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty()) {
            saveAuthTokens(context, accessToken, refreshToken)
            Log.d(TAG, "Токен успешно загружен из Cookie")
        } else {
            Log.e(TAG, "Ошибка: Cookie не содержит токен!")
        }
    }
}
