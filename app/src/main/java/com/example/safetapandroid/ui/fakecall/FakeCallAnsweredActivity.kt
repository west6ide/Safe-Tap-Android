package com.example.safetapandroid.ui.fakecall

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.airbnb.lottie.LottieAnimationView
import com.example.safetapandroid.R
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*

class FakeCallAnsweredActivity : AppCompatActivity() {

    private lateinit var timerView: TextView
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private var secondsElapsed = 0
    private var timer: CountDownTimer? = null
    private val client = OkHttpClient()
    private lateinit var role: String

    private val apiKey = "e68c1c6d72c14af8ad51f126d7c445ab"

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fake_call_answered)

        val name = intent.getStringExtra("name") ?: "Неизвестный"
        findViewById<TextView>(R.id.name).text = name
        timerView = findViewById(R.id.timer)
        role = intent.getStringExtra("role") ?: "assistant"

        findViewById<ImageButton>(R.id.btn_end_call).setOnClickListener {
            finish()
        }

        initTextToSpeech()
        initSpeechRecognizer()
        startTimer()
        startListening()
    }

    private fun showAIThinking(isThinking: Boolean) {
        val aiLoading = findViewById<LottieAnimationView>(R.id.ai_loading)
        aiLoading.visibility = if (isThinking) View.VISIBLE else View.GONE
    }

    private fun initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val spokenText = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                spokenText?.let {
                    showAIThinking(true)
                    sendToAIMLAPI(it)
                }
                startListening()
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { startListening() }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        speechRecognizer.startListening(intent)
    }

    private fun sendToAIMLAPI(userInput: String) {
        val systemPrompt = when (role.lowercase(Locale.ROOT)) {
            "мама" -> "Ты говоришь как мама — нежная, заботливая, волнуешься за ребёнка."
            "брат" -> "Ты говоришь как брат — дружелюбный, дерзкий, с братской поддержкой."
            "парень" -> "Ты говоришь как парень — нежно, романтично, с теплом."
            else -> "Ты помощник, общаешься по телефону."
        }

        val json = JSONObject().apply {
            put("model", "gpt-4")
            put("max_tokens", 100)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userInput)
                })
            })
        }

        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())

        val request = Request.Builder()
            .url("https://api.aimlapi.com/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread { showAIThinking(false) }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { body ->
                        val responseJson = JSONObject(body)
                        val reply = responseJson.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")

                        runOnUiThread {
                            speakOut(reply)
                            showAIThinking(false)
                        }
                    }
                }
            }
        })
    }

    private fun initTextToSpeech() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale("ru", "RU")
                val voices = tts.voices
                var preferredVoice: Voice? = null
                for (voice in voices) {
                    Log.d("TTS", "Voice: ${voice.name}, Locale: ${voice.locale}, Gender: ${voice.features}")
                }
                preferredVoice = when (role.lowercase(Locale.ROOT)) {
                    "мама" -> voices.find { it.name.contains("female", true) || it.name.contains("ru", true) }
                    "брат" -> voices.find { it.name.contains("male", true) || it.name.contains("ru", true) }
                    "парень" -> voices.find { it.name.contains("male", true) && it.name.contains("ru", true) }
                    else -> voices.find { it.locale.language == "ru" }
                }
                preferredVoice?.let { tts.voice = it }
            }
        }
    }

    private fun speakOut(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
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
        speechRecognizer.destroy()
        tts.shutdown()
    }
}
