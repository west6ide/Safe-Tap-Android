package com.example.safetapandroid.utils

import android.content.Context
import com.example.safetapandroid.network.FakeCall
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object FakeCallStorage {

    private const val PREFS_NAME = "fake_call_prefs"
    private const val KEY_CALLS = "calls"

    fun saveCalls(context: Context, calls: List<FakeCall>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(calls)
        prefs.edit().putString(KEY_CALLS, json).apply()
    }

    fun getCalls(context: Context): MutableList<FakeCall> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CALLS, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<FakeCall>>() {}.type
            Gson().fromJson(json, type)
        } else {
            mutableListOf()
        }
    }
}
