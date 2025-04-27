package com.example.safetapandroid.ui.fakecall

import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.safetapandroid.R

class FakeIncomingCallActivity : AppCompatActivity() {

    private lateinit var vibrator: Vibrator
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var acceptButton: ImageButton

    private var initialY = 0f
    private var dY = 0f
    private val SWIPE_THRESHOLD = 200 // порог свайпа

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fake_incoming_call)

        val name = intent.getStringExtra("name") ?: "Неизвестный"
        val number = intent.getStringExtra("number") ?: "+7 (777) 000-0000"

        findViewById<TextView>(R.id.name).text = name
        findViewById<TextView>(R.id.number).text = number
        acceptButton = findViewById(R.id.btn)
        acceptButton.elevation = 12f

        // Обрабатываем перемещение
        acceptButton.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = view.y
                    dY = view.y - event.rawY
                }

                MotionEvent.ACTION_MOVE -> {
                    val newY = event.rawY + dY
                    view.y = newY.coerceIn(initialY - 300, initialY + 300)
                }

                MotionEvent.ACTION_UP -> {
                    val delta = view.y - initialY
                    when {
                        delta < -SWIPE_THRESHOLD -> onSwipeUp()
                        delta > SWIPE_THRESHOLD -> onSwipeDown()
                        else -> view.animate().y(initialY).start()
                    }
                }
            }
            true
        }

        startVibration()
        startRingtone()
    }

    private fun startVibration() {
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 1000, 1000)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            vibrator.vibrate(1000)
        }
    }

    private fun startRingtone() {
        mediaPlayer = MediaPlayer.create(this, R.raw.ringtone)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
    }

    private fun stopCall() {
        if (::vibrator.isInitialized) vibrator.cancel()
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
    }

    private inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null || e2 == null) return false
            val diffY = e2.y - e1.y
            val diffX = e2.x - e1.x

            return if (Math.abs(diffY) > Math.abs(diffX)) {
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY < 0) {
                        onSwipeUp()
                    } else {
                        onSwipeDown()
                    }
                    true
                } else false
            } else false
        }
    }

    private fun onSwipeUp() {
        acceptButton.setBackgroundResource(R.drawable.bg_green_round_button)
        stopCall()
        val intent = Intent(this, FakeCallAnsweredActivity::class.java)
        intent.putExtra("name", findViewById<TextView>(R.id.name).text.toString())
        intent.putExtra("number", findViewById<TextView>(R.id.number).text.toString())
        startActivity(intent)
        Handler(Looper.getMainLooper()).postDelayed({
            playSelectedAudio()
        }, 3000)
        finish()
    }

    private fun playSelectedAudio() {
        val audioResId = intent.getIntExtra("audioResId", -1)
        if (audioResId != -1) {
            val mediaPlayer = MediaPlayer.create(this, audioResId)
            mediaPlayer.start()

            // Чтобы освободить ресурсы когда закончится
            mediaPlayer.setOnCompletionListener {
                mediaPlayer.release()
            }
        }
    }


    private fun onSwipeDown() {
        acceptButton.setBackgroundResource(R.drawable.bg_red_round_button)
        stopCall()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCall()
    }
}