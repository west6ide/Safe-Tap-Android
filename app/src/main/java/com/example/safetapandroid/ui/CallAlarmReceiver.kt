package com.example.safetapandroid.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.safetapandroid.network.FakeCall
import com.example.safetapandroid.ui.fakecall.FakeIncomingCallActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CallAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: "Неизвестный"
        val number = intent.getStringExtra("number") ?: "+7 777 777 77 77"
        val photoUrl = intent.getStringExtra("photoUrl") ?: ""
        val selectedRole = intent.getStringExtra("role") ?: "assistant"

        // Запуск экрана входящего вызова
        val callIntent = Intent(context, FakeIncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("name", name)
            putExtra("number", number)
            putExtra("photoUrl", photoUrl)
            putExtra("role", selectedRole) // ✅ передаем роль
        }
        context.startActivity(callIntent)

        // --- УДАЛЕНИЕ ПРОШЕДШЕГО ФЕЙКОВОГО ЗВОНКА ИЗ СПИСКА ---
        removeExecutedCall(context, name, number)
    }

    private fun removeExecutedCall(context: Context, name: String, number: String) {
        val prefs = context.getSharedPreferences("fake_calls", Context.MODE_PRIVATE)
        val json = prefs.getString("list", null) ?: return

        val type = object : TypeToken<MutableList<FakeCall>>() {}.type
        val fakeCalls: MutableList<FakeCall> = Gson().fromJson(json, type)

        val updatedCalls = fakeCalls.filterNot { it.name == name && it.number == number }

        val newJson = Gson().toJson(updatedCalls)
        prefs.edit().putString("list", newJson).apply()
    }
}
