package com.example.safetapandroid.model

data class ContactRequest(val phone_number: String)

data class TrustedContact(
    val id: Int,
    val user_id: Int,
    val phone_number: String
)