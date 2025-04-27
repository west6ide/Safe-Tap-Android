package com.example.safetapandroid.model

data class User(
    val phone: String,
    val password: String
)

data class RegisterUser(
    val name: String,
    val phone: String,
    val password: String,
    val confirmPassword: String
)

data class AuthResponse(
    val token: String
)