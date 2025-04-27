package com.example.safetapandroid.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.safetapandroid.R
import com.example.safetapandroid.network.AuthApi
import com.example.safetapandroid.network.Notification
import com.example.safetapandroid.network.RetrofitClient
import com.example.safetapandroid.utils.UserManager
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationActivity : AppCompatActivity() {

    private lateinit var notificationContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        notificationContainer = findViewById(R.id.notificationContainer)
        fetchNotifications()
    }

    private fun fetchNotifications() {
        val api = RetrofitClient.getInstance(this).create(AuthApi::class.java)
        val token = UserManager.getAuthToken(this)

        if (token == null) {
            Toast.makeText(this, "Authentication token not found!", Toast.LENGTH_SHORT).show()
            return
        }

        api.getNotifications("Bearer $token").enqueue(object : Callback<List<Notification>> {
            override fun onResponse(call: Call<List<Notification>>, response: Response<List<Notification>>) {
                if (response.isSuccessful) {
                    val notifications = response.body() ?: emptyList()
                    displayNotifications(notifications)
                } else {
                    Toast.makeText(this@NotificationActivity, "Failed to fetch notifications", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Notification>>, t: Throwable) {
                Toast.makeText(this@NotificationActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayNotifications(notifications: List<Notification>) {
        notificationContainer.removeAllViews()

        for (notification in notifications) {
            val notificationView = layoutInflater.inflate(R.layout.item_notification, null)

            val titleTextView = notificationView.findViewById<TextView>(R.id.notificationTitle)
            val messageTextView = notificationView.findViewById<TextView>(R.id.notificationMessage)
            val openMapButton = notificationView.findViewById<Button>(R.id.btnOpenMap)
            val callButton = notificationView.findViewById<Button>(R.id.btnCall)

            titleTextView.text = "Attention! SOS Signal Received"
            messageTextView.text = notification.message

            // Extract the latitude and longitude from the notification message
            val latitude = notification.latitude
            val longitude = notification.longitude

            openMapButton.setOnClickListener {
                val intent = Intent(this@NotificationActivity, MapsActivity::class.java)
                intent.putExtra("notification_latitude", latitude)
                intent.putExtra("notification_longitude", longitude)
                startActivity(intent)
            }


            callButton.setOnClickListener {
                Toast.makeText(this, "Calling emergency contact...", Toast.LENGTH_SHORT).show()
            }

            notificationContainer.addView(notificationView)
        }
    }
}
