package com.example.safetapandroid.ui

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.safetapandroid.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class RoutePreviewActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment

    private var startLat = 0.0
    private var startLng = 0.0
    private var destLat = 0.0
    private var destLng = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_preview)

        startLat = intent.getDoubleExtra("start_lat", 0.0)
        startLng = intent.getDoubleExtra("start_lng", 0.0)
        destLat = intent.getDoubleExtra("dest_lat", 0.0)
        destLng = intent.getDoubleExtra("dest_lng", 0.0)

        mapFragment = supportFragmentManager.findFragmentById(R.id.map_preview) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val start = LatLng(startLat, startLng)
        val end = LatLng(destLat, destLng)

        mMap.addMarker(MarkerOptions().position(start).title("Начало"))
        mMap.addMarker(MarkerOptions().position(end).title("Конец"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 13f))

        drawRoute(start, end)
    }

    private fun drawRoute(start: LatLng, end: LatLng) {
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${start.latitude},${start.longitude}" +
                "&destination=${end.latitude},${end.longitude}" +
                "&key=AIzaSyANf9wnCcRFRslApgQTjqYhDLOg6nIQ9-E"

        Thread {
            val request = Request.Builder().url(url).build()
            val response = OkHttpClient().newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: return@Thread)

            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) return@Thread

            val steps = routes.getJSONObject(0).getJSONArray("legs")
                .getJSONObject(0).getJSONArray("steps")

            val points = mutableListOf<LatLng>()
            for (i in 0 until steps.length()) {
                val polyline = steps.getJSONObject(i).getJSONObject("polyline").getString("points")
                points.addAll(decodePolyline(polyline))
            }

            runOnUiThread {
                mMap.addPolyline(
                    PolylineOptions().addAll(points).width(10f).color(Color.BLUE)
                )
            }
        }.start()
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

            val p = LatLng(lat / 1E5, lng / 1E5)
            poly.add(p)
        }
        return poly
    }
}
