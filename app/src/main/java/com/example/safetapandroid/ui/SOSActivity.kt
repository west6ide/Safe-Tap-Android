package com.example.safetapandroid.ui

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.safetapandroid.R
import com.example.safetapandroid.network.AuthApi
import com.example.safetapandroid.network.RetrofitClient
import com.example.safetapandroid.network.SOSRequest
import com.example.safetapandroid.utils.UserManager
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SOSActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sos)

        val btnSendSos = findViewById<Button>(R.id.btn_send_sos)
        btnSendSos.setOnClickListener {
            val latitude = intent.getDoubleExtra("latitude", 0.0)
            val longitude = intent.getDoubleExtra("longitude", 0.0)

            if (latitude == 0.0 && longitude == 0.0) {
                Toast.makeText(this, "Coordinates not received!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val api = RetrofitClient.getInstance(this).create(AuthApi::class.java)
            val token = UserManager.getAuthToken(this)

            if (token.isNullOrEmpty()) {
                Toast.makeText(this, "Authorization token not found!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val request = SOSRequest(latitude = latitude, longitude = longitude)

            api.sendSOS("Bearer $token", request).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@SOSActivity, "SOS successfully sent!", Toast.LENGTH_SHORT).show()
                        Log.d("SOSActivity", "Response: ${response.body()?.string()}")
                    } else {
                        Toast.makeText(this@SOSActivity, "Failed to send SOS: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@SOSActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
