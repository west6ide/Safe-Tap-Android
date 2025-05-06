package com.example.safetapandroid.ui.fakecall

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.safetapandroid.R
import com.example.safetapandroid.network.FakeCall
import com.example.safetapandroid.ui.CallAlarmReceiver
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

class FakeCallSchedulerActivity : AppCompatActivity() {

    private lateinit var nameInput: EditText
    private lateinit var numberInput: EditText
    private lateinit var timeButton: Button
    private lateinit var fakeCallsRecyclerView: RecyclerView
    private lateinit var adapter: FakeCallAdapter
    private lateinit var emptyText: TextView
    private var selectedAudioResId: Int? = null
    private lateinit var roleSpinner: Spinner

    private var scheduledHour = 0
    private var scheduledMinute = 0

    private val fakeCalls = mutableListOf<FakeCall>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fake_call_scheduler)

        nameInput = findViewById(R.id.input_name)
        numberInput = findViewById(R.id.input_number)
        timeButton = findViewById(R.id.button_pick_time)
        fakeCallsRecyclerView = findViewById(R.id.fake_calls_list)
        emptyText = findViewById(R.id.empty_text)
        roleSpinner = findViewById(R.id.role_spinner)


        adapter = FakeCallAdapter(
            fakeCalls,
            onDelete = ::deleteFakeCall,
            onEdit = ::editFakeCall
        )
        fakeCallsRecyclerView.layoutManager = LinearLayoutManager(this)
        fakeCallsRecyclerView.adapter = adapter
        fakeCallsRecyclerView.itemAnimator = DefaultItemAnimator()

        loadFakeCalls()

        timeButton.setOnClickListener {
            val now = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                scheduledHour = hour
                scheduledMinute = minute
                scheduleFakeCall()
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
        }
        roleSpinner = findViewById(R.id.role_spinner)
        val roles = listOf("Мама", "Брат", "Парень")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
        roleSpinner.adapter = adapter
    }

    private fun showAudioSelectionDialog() {
        val rawClass = R.raw::class.java
        val fields = rawClass.fields

        val audioTitles = mutableListOf<String>()
        val audioResIds = mutableListOf<Int>()

        for (field in fields) {
            val resId = field.getInt(null)
            audioResIds.add(resId)
            audioTitles.add(field.name.replace("_", " ").capitalize())
        }

        if (audioTitles.isEmpty()) {
            Toast.makeText(this, "Нет доступных аудиофайлов", Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Выберите аудио")

        builder.setItems(audioTitles.toTypedArray()) { _, which ->
            selectedAudioResId = audioResIds[which]
            Toast.makeText(this, "Вы выбрали: ${audioTitles[which]}", Toast.LENGTH_SHORT).show()
        }

        builder.show()
    }



    private fun scheduleFakeCall() {
        val name = nameInput.text.toString()
        val number = numberInput.text.toString()

        if (name.isBlank() || number.isBlank()) {
            Toast.makeText(this, "Заполните имя и номер", Toast.LENGTH_SHORT).show()
            return
        }
        val selectedRole = roleSpinner.selectedItem.toString()
        val fakeCall = FakeCall(
            name = name,
            number = number,
            hour = scheduledHour,
            minute = scheduledMinute,
            role = selectedRole
        )

        fakeCalls.add(fakeCall)
        saveFakeCalls()
        adapter.updateList(fakeCalls)

        // ДОБАВЬ ЭТО: скрывать/показывать пустой текст
        emptyText.visibility = if (fakeCalls.isEmpty()) View.VISIBLE else View.GONE

        val intent = Intent(this, CallAlarmReceiver::class.java).apply {
            putExtra("name", name)
            putExtra("number", number)
            putExtra("role", selectedRole)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this, fakeCall.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, scheduledHour)
            set(Calendar.MINUTE, scheduledMinute)
            set(Calendar.SECOND, 0)
        }

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)

        Toast.makeText(this, "Фейковый звонок запланирован", Toast.LENGTH_SHORT).show()

        // Очистить поля
        nameInput.text.clear()
        numberInput.text.clear()
    }

    private fun editFakeCall(fakeCall: FakeCall) {
        nameInput.setText(fakeCall.name)
        numberInput.setText(fakeCall.number)
        scheduledHour = fakeCall.hour
        scheduledMinute = fakeCall.minute

        fakeCalls.removeIf { it.id == fakeCall.id }
        saveFakeCalls()
        adapter.updateList(fakeCalls)
    }

    private fun deleteFakeCall(fakeCall: FakeCall) {
        fakeCalls.removeIf { it.id == fakeCall.id }
        saveFakeCalls()
        adapter.updateList(fakeCalls)
        emptyText.visibility = if (fakeCalls.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadFakeCalls() {
        val prefs = getSharedPreferences("fake_calls", MODE_PRIVATE)
        val json = prefs.getString("list", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<FakeCall>>() {}.type
            fakeCalls.addAll(Gson().fromJson(json, type))
        }
    }

    private fun saveFakeCalls() {
        val prefs = getSharedPreferences("fake_calls", MODE_PRIVATE).edit()
        val json = Gson().toJson(fakeCalls)
        prefs.putString("list", json)
        prefs.apply()
    }

    override fun onResume() {
        super.onResume()
        reloadFakeCalls()
    }

    private fun reloadFakeCalls() {
        val prefs = getSharedPreferences("fake_calls", MODE_PRIVATE)
        val json = prefs.getString("list", null)
        fakeCalls.clear() // <-- Обязательно очищаем
        if (json != null) {
            val type = object : TypeToken<MutableList<FakeCall>>() {}.type
            fakeCalls.addAll(Gson().fromJson(json, type))
        }
        adapter.updateList(fakeCalls)
        emptyText.visibility = if (fakeCalls.isEmpty()) View.VISIBLE else View.GONE
    }
}