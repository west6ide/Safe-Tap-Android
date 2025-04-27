package com.example.safetapandroid.ui.fakecall

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.safetapandroid.R

class FakeCallAnsweredActivity : AppCompatActivity() {

    private lateinit var timerView: TextView
    private var secondsElapsed = 0
    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) // отключает тёмную тему
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fake_call_answered)

        val name = intent.getStringExtra("name") ?: "Неизвестный"


        findViewById<TextView>(R.id.name).text = name
        timerView = findViewById(R.id.timer)

        findViewById<ImageButton>(R.id.btn_end_call).setOnClickListener {
            finish()
        }

        startTimer()
    }

    private fun startTimer() {
        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                secondsElapsed++
                val minutes = secondsElapsed / 60
                val seconds = secondsElapsed % 60
                timerView.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {}
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}