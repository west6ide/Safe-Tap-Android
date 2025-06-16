package com.example.safetapandroid.ui

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.safetapandroid.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class RoutePreviewActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var startLat = 0.0
    private var startLng = 0.0
    private var destLat = 0.0
    private var destLng = 0.0
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_preview)

        // Get coordinates from intent
        startLat = intent.getDoubleExtra("start_lat", 0.0)
        startLng = intent.getDoubleExtra("start_lng", 0.0)
        destLat = intent.getDoubleExtra("dest_lat", 0.0)
        destLng = intent.getDoubleExtra("dest_lng", 0.0)

        // Verify coordinates are valid
        if (startLat == 0.0 || startLng == 0.0 || destLat == 0.0 || destLng == 0.0) {
            Toast.makeText(this, "Invalid route coordinates", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_preview) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val start = LatLng(startLat, startLng)
        val end = LatLng(destLat, destLng)

        // Add markers
        mMap.addMarker(MarkerOptions().position(start).title("Start"))
        mMap.addMarker(MarkerOptions().position(end).title("End"))

        // Center camera on route
        val bounds = LatLngBounds.Builder()
            .include(start)
            .include(end)
            .build()
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))

        // Draw route
        drawRoute(start, end)
    }

    private fun drawRoute(start: LatLng, end: LatLng) {
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${start.latitude},${start.longitude}" +
                "&destination=${end.latitude},${end.longitude}" +
                "&key=AIzaSyANf9wnCcRFRslApgQTjqYhDLOg6nIQ9-E"

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("RoutePreview", "Failed to fetch route", e)
                runOnUiThread {
                    Toast.makeText(this@RoutePreviewActivity, "Failed to load route", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    try {
                        val json = JSONObject(responseBody.string())
                        val routes = json.getJSONArray("routes")
                        if (routes.length() > 0) {
                            val route = routes.getJSONObject(0)
                            val polyline = route.getJSONObject("overview_polyline").getString("points")
                            val decodedPath = decodePolyline(polyline)

                            runOnUiThread {
                                mMap.addPolyline(
                                    PolylineOptions()
                                        .addAll(decodedPath)
                                        .width(10f)
                                        .color(Color.BLUE)
                                )
                            }
                        } else {

                        }
                    } catch (e: Exception) {
                        Log.e("RoutePreview", "Error parsing route", e)
                    }
                }
            }
        })
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            poly.add(LatLng(lat / 1E5, lng / 1E5))
        }
        return poly
    }
}