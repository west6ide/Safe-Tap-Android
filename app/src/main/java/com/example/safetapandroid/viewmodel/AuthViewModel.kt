package com.example.safetapandroid.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.safetapandroid.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application, private val repository: AuthRepository) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val _isAuthenticated = MutableStateFlow(isUserLoggedIn())
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    fun isUserLoggedIn(): Boolean {
        val token = sharedPreferences.getString("token", null)
        return !token.isNullOrEmpty() // ✅ Проверяем, есть ли токен
    }

    fun authenticateWithGoogle(idToken: String) {
        if (idToken.isNotEmpty()) {
            sharedPreferences.edit().putString("token", idToken).apply()
            _isAuthenticated.value = true // ✅ Теперь состояние обновляется автоматически
        }
        else {
            _isAuthenticated.value = false
        }
    }

    fun loginUser(phone: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            repository.loginUser(phone, password) { success, token ->
                if (success && token != null) {
                    sharedPreferences.edit().putString("token", token).apply()
                    _isAuthenticated.value = true
                }
                else {
                    _isAuthenticated.value = false
                }
                onResult(success)
            }
        }
    }

    fun registerUser(name: String, phoneNumber: String, password: String, confirmPassword: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            repository.registerUser(name, phoneNumber, password, confirmPassword) { success, token ->
                if (success && token != null) {
                    sharedPreferences.edit().putString("token", token).apply()
                    _isAuthenticated.value= true
                }
                else {
                    _isAuthenticated.value = false
                }
                onResult(success)
            }
        }
    }




    fun logout() {
        sharedPreferences.edit().remove("token").apply()
        _isAuthenticated.value = false // ✅ Теперь состояние обновляется при выходе
    }
}
