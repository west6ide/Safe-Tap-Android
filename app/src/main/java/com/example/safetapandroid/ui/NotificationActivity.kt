package com.example.safetapandroid.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.safetapandroid.R
import com.example.safetapandroid.network.AuthApi
import com.example.safetapandroid.network.Notification
import com.example.safetapandroid.network.RetrofitClient
import com.example.safetapandroid.network.SharedRoute
import com.example.safetapandroid.utils.UserManager
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationActivity : AppCompatActivity() {

    private lateinit var notificationContainer: LinearLayout
    private val api by lazy { RetrofitClient.getInstance(this).create(AuthApi::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        findViewById<ImageButton>(R.id.btnCloseNotification).setOnClickListener { finish() }
        notificationContainer = findViewById(R.id.notificationContainer)

        fetchAllNotifications()
    }

    private fun fetchAllNotifications() {
        val token = UserManager.getAuthToken(this) ?: run {
            Toast.makeText(this, "Authentication token not found!", Toast.LENGTH_SHORT).show()
            return
        }

        api.getNotifications("Bearer $token").enqueue(object : Callback<List<Notification>> {
            override fun onResponse(call: Call<List<Notification>>, response: Response<List<Notification>>) {
                if (response.isSuccessful) {
                    response.body()?.let { notifications ->
                        if (notifications.isEmpty()) {
                            showEmptyState()
                        } else {
                            displayAllNotifications(notifications)
                        }
                    }
                } else {
                    Toast.makeText(
                        this@NotificationActivity,
                        "Failed to fetch notifications",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<List<Notification>>, t: Throwable) {
                Toast.makeText(
                    this@NotificationActivity,
                    "Error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun showEmptyState() {
        notificationContainer.removeAllViews()
        val emptyView = layoutInflater.inflate(R.layout.item_empty_state, null).apply {
            findViewById<TextView>(R.id.emptyText).text = "No notifications yet"
        }
        notificationContainer.addView(emptyView)
    }

    private fun displayAllNotifications(notifications: List<Notification>) {
        notificationContainer.removeAllViews()

        notifications.sortedByDescending { it.createdAt }.forEach { notification ->
            when (notification.type) {
                "sos" -> displaySosNotification(notification)
                "route" -> displayRouteNotification(notification)
                else -> displayGenericNotification(notification)
            }
        }
    }

    private fun displaySosNotification(notification: Notification) {
        val view = layoutInflater.inflate(R.layout.item_notification, null).apply {
            findViewById<TextView>(R.id.notificationTitle).text = "SOS Alert"
            findViewById<TextView>(R.id.notificationMessage).text = notification.message

            findViewById<Button>(R.id.btnOpenMap).setOnClickListener {
                startActivity(Intent(this@NotificationActivity, MapsActivity::class.java).apply {
                    putExtra("notification_latitude", notification.latitude)
                    putExtra("notification_longitude", notification.longitude)
                })
            }

            findViewById<Button>(R.id.btnCall).setOnClickListener {
                Toast.makeText(
                    this@NotificationActivity,
                    "Calling emergency contact...",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        notificationContainer.addView(view)
    }

    private fun displayRouteNotification(notification: Notification) {
        val view = layoutInflater.inflate(R.layout.item_notification, null).apply {
            findViewById<TextView>(R.id.notificationTitle).text = "Shared Route"
            findViewById<TextView>(R.id.notificationMessage).text = notification.message

            findViewById<Button>(R.id.btnOpenMap).setOnClickListener {
                if (notification.destLatitude == null || notification.destLongitude == null) {
                    Toast.makeText(
                        this@NotificationActivity,
                        "Route destination not available",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val intent = Intent(this@NotificationActivity, MapsActivity::class.java).apply {
                    putExtra("show_route_in_panel", true)
                    putExtra("start_lat", notification.latitude)
                    putExtra("start_lng", notification.longitude)
                    putExtra("dest_lat", notification.destLatitude)
                    putExtra("dest_lng", notification.destLongitude)
                    putExtra("duration", notification.duration ?: "")
                    putExtra("distance", notification.distance ?: "")
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                finish()
            }
            findViewById<Button>(R.id.btnCall).visibility = View.GONE
        }
        notificationContainer.addView(view)
    }

    private fun displayGenericNotification(notification: Notification) {
        val view = layoutInflater.inflate(R.layout.item_notification, null).apply {
            findViewById<TextView>(R.id.notificationTitle).text = "Notification"
            findViewById<TextView>(R.id.notificationMessage).text = notification.message

            findViewById<Button>(R.id.btnOpenMap).visibility = View.GONE
            findViewById<Button>(R.id.btnCall).visibility = View.GONE
        }
        notificationContainer.addView(view)
    }

    private fun extractRouteInfo(message: String): Pair<String, String> {
        // Example message format: "Route shared: 15 mins (2.5 km)"
        val durationRegex = Regex("""(\d+\s*(mins?|hours?|hrs?))""")
        val distanceRegex = Regex("""(\d+\.?\d*\s*(km|miles?))""")

        val duration = durationRegex.find(message)?.value ?: ""
        val distance = distanceRegex.find(message)?.value ?: ""

        return Pair(duration, distance)
    }

}