package com.example.safetapandroid.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager private constructor(context: Context) { // ✅ Приватный конструктор
    private val prefs: SharedPreferences = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString("TOKEN", token).apply()
    }

    fun getToken(): String? {
        return prefs.getString("TOKEN", null)
    }

    companion object {
        @Volatile private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun saveLocation(lat: Double, lng: Double) {
        prefs.edit().putFloat("LATITUDE", lat.toFloat()).putFloat("LONGITUDE", lng.toFloat()).apply()
    }

    fun getLatitude(): Double? {
        return prefs.getFloat("LATITUDE", 0.0f).toDouble()
    }

    fun getLongitude(): Double? {
        return prefs.getFloat("LONGITUDE", 0.0f).toDouble()
    }

}
