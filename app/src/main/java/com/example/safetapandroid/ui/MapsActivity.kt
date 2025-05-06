package com.example.safetapandroid.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.safetapandroid.MainActivity
import com.example.safetapandroid.R
import com.example.safetapandroid.viewmodel.AuthViewModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import android.widget.ImageButton
import android.widget.TextView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.libraries.places.api.Places
import android.widget.SearchView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import com.example.safetapandroid.network.AuthApi
import com.example.safetapandroid.network.LiveLocation
import com.example.safetapandroid.network.RetrofitClient
import com.example.safetapandroid.network.SOSRequest
import com.example.safetapandroid.utils.UserManager
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.net.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.safetapandroid.network.DangerousPerson
import com.example.safetapandroid.ui.fakecall.FakeCallSchedulerActivity
import com.google.android.libraries.places.api.model.RectangularBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import okhttp3.ResponseBody



class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentMarker: Marker? = null
    private val client = OkHttpClient()
    private lateinit var placesClient: PlacesClient
    private lateinit var searchView: SearchView
    private var destinationMarker: Marker? = null
    private var currentLocation: Location? = null
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0

    private var isFromNotification: Boolean = false
    private var sosLatitude: Double? = null
    private var sosLongitude: Double? = null

    private var isDangerousPlacesVisible = false
    private val dangerousMarkers = mutableListOf<Marker>()
    private var isSafePlacesVisible = false
    private val nearbyPlaceMarkers = mutableListOf<Marker>()




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        findViewById<ImageButton>(R.id.btn_show_search).visibility = View.GONE

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyANf9wnCcRFRslApgQTjqYhDLOg6nIQ9-E")
        }
        placesClient = Places.createClient(this)

//        setupSearchView()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { updateLocationOnMap(it) }
            }
        }

        findViewById<LinearLayout>(R.id.sign_out_button).setOnClickListener { signOut() }
        findViewById<ImageButton>(R.id.btn_sos).setOnClickListener {
            if (currentLatitude != 0.0 && currentLongitude != 0.0) {
                val intent = Intent(this, SOSActivity::class.java)
                intent.putExtra("latitude", currentLatitude)
                intent.putExtra("longitude", currentLongitude)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Координаты не получены!", Toast.LENGTH_LONG).show()
            }
        }

        findViewById<ImageButton>(R.id.btn_fake_call).setOnClickListener {
            val intent = Intent(this, FakeCallSchedulerActivity::class.java)
            startActivity(intent)
        }
        findViewById<ImageButton>(R.id.btn_my_location).setOnClickListener { moveToMyLocation() }
        findViewById<ImageButton>(R.id.btn_notifications).setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val btnOptions = findViewById<ImageButton>(R.id.btn_options)

        btnOptions.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        findViewById<ImageButton>(R.id.btn_emergency_contacts).setOnClickListener {
            if (isDangerousPlacesVisible) {
                dangerousMarkers.forEach { it.remove() }
                dangerousMarkers.clear()
                isDangerousPlacesVisible = false
            } else {
                fetchDangerousPlacesFromApi {
                    isDangerousPlacesVisible = true
                }
            }
        }

        findViewById<ImageButton>(R.id.btn_show_search).setOnClickListener {
            findViewById<ConstraintLayout>(R.id.bottom_menu).visibility = View.VISIBLE
            findViewById<ConstraintLayout>(R.id.route_info_panel).visibility = View.GONE
            it.visibility = View.GONE // скрыть саму кнопку
        }


        // Добавим обработку кнопки для показа/скрытия безопасных мест
        findViewById<ImageButton>(R.id.btn_nearby_safe_places).setOnClickListener {
            if (isSafePlacesVisible) {
                nearbyPlaceMarkers.forEach { it.remove() }
                nearbyPlaceMarkers.clear()
                isSafePlacesVisible = false
            } else {
                showSafePlacesInCurrentCity {
                    isSafePlacesVisible = true
                }
            }
        }


//        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//            override fun onQueryTextSubmit(query: String?): Boolean {
//                query?.let { searchLocation(it) }
//                return false
//            }
//
//            override fun onQueryTextChange(newText: String?): Boolean = false
//        })

//        setupAutocomplete()

        // Получаем координаты из уведомления
        sosLatitude = intent.getDoubleExtra("notification_latitude", 0.0)
        sosLongitude = intent.getDoubleExtra("notification_longitude", 0.0)

        isFromNotification = sosLatitude != 0.0 && sosLongitude != 0.0

        val geocoder = Geocoder(this)
        val addresses = geocoder.getFromLocation(currentLatitude, currentLongitude, 1)
        val cityName = addresses?.firstOrNull()?.locality ?: ""

        val spinner = findViewById<Spinner>(R.id.transport_mode_spinner)
        val btnBuildRoute = findViewById<Button>(R.id.btn_build_route)

// Настраиваем Spinner
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.transport_modes,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        btnBuildRoute.setOnClickListener {
            val mode = when (spinner.selectedItem.toString()) {
                "Авто" -> "driving"
                "Пешком" -> "walking"
                "Общественный транспорт" -> "transit"
                else -> "driving"
            }

            val destination = destinationMarker?.position
            if (currentLocation != null && destination != null) {
                drawRoute(currentLocation!!, destination, mode)
            } else {
                Toast.makeText(this, "Маршрут не может быть построен", Toast.LENGTH_SHORT).show()
            }
        }


        setupAutocompleteSearch(cityName)

    }

    private fun setupAutocompleteSearch(city: String) {
        val autocompleteFragment = supportFragmentManager
            .findFragmentById(R.id.place_autocomplete_fragment) as AutocompleteSupportFragment

        autocompleteFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG
            )
        )

        val center = LatLng(currentLatitude, currentLongitude)
        val radiusInMeters = 20000.0 // 20 км

        val bounds = RectangularBounds.newInstance(
            LatLng(center.latitude - radiusInMeters / 111000, center.longitude - radiusInMeters / 111000),
            LatLng(center.latitude + radiusInMeters / 111000, center.longitude + radiusInMeters / 111000)
        )
        autocompleteFragment.setLocationBias(bounds)

        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                val latLng = place.latLng
                if (latLng != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    destinationMarker?.remove()
                    destinationMarker = mMap.addMarker(
                        MarkerOptions().position(latLng).title(place.name)
                    )

                    findViewById<ConstraintLayout>(R.id.bottom_menu).visibility = View.GONE
                }
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                Log.e("MapsActivity", "Ошибка выбора места: $status")
                Toast.makeText(this@MapsActivity, "Ошибка поиска: $status", Toast.LENGTH_SHORT).show()
            }
        })
    }





    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        checkPermissions()

        val notificationLatitude = intent.getDoubleExtra("notification_latitude", 0.0)
        val notificationLongitude = intent.getDoubleExtra("notification_longitude", 0.0)

        if (notificationLatitude != 0.0 && notificationLongitude != 0.0) {
            val sosLocation = LatLng(notificationLatitude, notificationLongitude)
            mMap.addMarker(
                MarkerOptions()
                    .position(sosLocation)
                    .title("SOS Signal Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(sosLocation, 15f))
        } else {
            startLocationUpdates() // Только если SOS координаты не переданы
        }

        mMap.setOnMapClickListener { latLng ->
            destinationMarker?.remove()
            destinationMarker = mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Пункт назначения")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            )
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

            // Скрыть нижнее меню
            findViewById<ConstraintLayout>(R.id.bottom_menu).visibility = View.GONE

// Показать кнопку поиска обратно
            findViewById<ImageButton>(R.id.btn_show_search).visibility = View.VISIBLE

// Построить маршрут и показать панель
            val destination = destinationMarker?.position
            if (currentLocation != null && destination != null) {
                drawRoute(currentLocation!!, destination, "driving") // default mode
            }

        }


        startEmergencyLocationUpdates()
    }

    private fun showSafePlacesInCurrentCity(onComplete: () -> Unit) {
        val geocoder = Geocoder(this)
        try {
            val addresses = geocoder.getFromLocation(currentLatitude, currentLongitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val city = addresses[0].locality ?: addresses[0].subAdminArea ?: "Unknown"
                Log.d("MapsActivity", "City detected: $city")

                fetchNearbyPlaces(city) {
                    fetchNearbyPolicePlaces(city) {
                        fetchNearbyMedicalPlaces(city) {
                            onComplete()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Город не найден", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("MapsActivity", "Geocoder error: ${e.message}")
            Toast.makeText(this, "Ошибка определения города", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getBitmapFromDrawable(resId: Int): Bitmap? {
        var bitmap : Bitmap? = null
        val drawable = ResourcesCompat.getDrawable(resources, resId, null)
        if (drawable == null) {
            Log.e("MapsActivity", "Drawable не найден для ресурса: $resId")
        }

        if(drawable != null){
            bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0,0,canvas.width,canvas.height)
            drawable.draw(canvas)
        }
        return bitmap
    }



    private fun startEmergencyLocationUpdates() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                fetchEmergencyLocations()
                handler.postDelayed(this, 10000)
            }
        }
        handler.post(runnable)
    }

    private fun fetchEmergencyLocations() {
        val token = UserManager.getAuthToken(this) ?: return
        val api = RetrofitClient.getInstance(this).create(AuthApi::class.java)
        api.getEmergencyLocations("Bearer $token").enqueue(object : Callback<List<LiveLocation>> {
            override fun onResponse(call: Call<List<LiveLocation>>, response: Response<List<LiveLocation>>) {
                if (response.isSuccessful) {
                    val data = response.body() ?: return
                    showContactMarkers(data)
                }
            }

            override fun onFailure(call: Call<List<LiveLocation>>, t: Throwable) {
                Log.e("MapsActivity", "Error fetching emergency locations: ${t.message}")
            }
        })
    }

    private val contactMarkers = mutableMapOf<Int, Marker>()

    private fun showContactMarkers(locations: List<LiveLocation>) {
        val existingIds = locations.map { it.userId }.toSet()

        // Удаляем маркеры, которых больше нет в данных
        val toRemove = contactMarkers.keys - existingIds
        toRemove.forEach {
            contactMarkers[it]?.remove()
            contactMarkers.remove(it)
        }

        // Обновляем или добавляем маркеры
        for (location in locations) {
            val latLng = LatLng(location.latitude, location.longitude)
            val marker = contactMarkers[location.userId]
            if (marker == null) {
                val newMarker = mMap.addMarker(
                    MarkerOptions().position(latLng).title("Контакт: ${location.name ?: "Без имени"}")
                )
                if (newMarker != null) {
                    contactMarkers[location.userId] = newMarker
                }
            } else {
                marker.position = latLng
            }
        }
    }







    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            enableLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_REQUEST_CODE
            )
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                enableLocation()
                startLocationUpdates()
            } else {
                Toast.makeText(this, "Разрешение на геолокацию не предоставлено", Toast.LENGTH_LONG).show()
            }
        }
    }



    @SuppressLint("MissingPermission")
    private fun enableLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                mMap.isMyLocationEnabled = true
            } catch (e: SecurityException) {
                Log.e("MapsActivity", "SecurityException при включении MyLocation: ${e.message}")
            }
        }
    }



    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            val request = LocationRequest.create().apply {
                interval = 10000
                fastestInterval = 5000
                priority = Priority.PRIORITY_HIGH_ACCURACY
            }
            try {
                fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
            } catch (e: SecurityException) {
                Log.e("MapsActivity", "SecurityException при запуске обновления геолокации: ${e.message}")
            }
        }
    }




    private fun updateLocationOnMap(location: Location) {
        currentLatitude = location.latitude
        currentLongitude = location.longitude
        currentLocation = location

        val latLng = LatLng(currentLatitude, currentLongitude)

        if (currentMarker == null) {
            currentMarker = mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("You are here")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
        } else {
            currentMarker?.position = latLng
        }

        sendMyLocationToServer(currentLatitude, currentLongitude)
    }



    private fun sendMyLocationToServer(lat: Double, lon: Double) {
        val token = UserManager.getAuthToken(this) ?: return
        val request = SOSRequest(lat, lon)

        val api = RetrofitClient.getInstance(this).create(AuthApi::class.java)
        api.updateLocation("Bearer $token", request).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {}
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {}
        })
    }


    private fun moveToMyLocation() {
        if (currentLocation != null) {
            val latLng = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)

            if (currentMarker == null) {
                currentMarker = mMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title("You are here")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                )
            } else {
                currentMarker?.position = latLng
            }

            // ✅ Оставляем перемещение камеры только при нажатии кнопки
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        } else {
            Toast.makeText(this, "Текущее местоположение недоступно", Toast.LENGTH_SHORT).show()
        }
    }



    private fun getUserName(): String {
        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        return prefs.getString("user_name", "User") ?: "User"
    }


//    private fun setupSearchView() {
//        val autocompleteFragment =
//            supportFragmentManager.findFragmentById(R.id.place_autocomplete_fragment) as AutocompleteSupportFragment
//
//        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))
//
//        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
//            override fun onPlaceSelected(place: Place) {
//                place.latLng?.let { latLng ->
//                    mMap.clear()
//                    mMap.addMarker(MarkerOptions().position(latLng).title(place.name))
//                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
//                }
//            }
//
//            override fun onError(status: com.google.android.gms.common.api.Status) {
//                Log.e("MapsActivity", "Place error: $status")
//            }
//        })
//    }



    private fun searchLocation(query: String) {
        val token = AutocompleteSessionToken.newInstance()
        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(token)
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                response.autocompletePredictions.firstOrNull()?.let {
                    fetchPlaceDetails(it.placeId)
                }
            }
    }


    private fun fetchPlaceDetails(placeId: String) {
        val request = FetchPlaceRequest.builder(placeId, listOf(Place.Field.LAT_LNG)).build()
        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                response.place.latLng?.let { latLng ->
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    destinationMarker?.remove()
                    destinationMarker = mMap.addMarker(MarkerOptions().position(latLng).title("Выбрано"))
                }
            }
    }


    private fun drawRoute(start: Location, destination: LatLng, mode: String) {
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${start.latitude},${start.longitude}" +
                "&destination=${destination.latitude},${destination.longitude}" +
                "&mode=$mode&key=AIzaSyANf9wnCcRFRslApgQTjqYhDLOg6nIQ9-E"

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("MapsActivity", "Ошибка маршрута: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.body?.let { responseBody ->
                    val json = JSONObject(responseBody.string())
                    val routes = json.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val points = mutableListOf<LatLng>()
                        val leg = routes.getJSONObject(0).getJSONArray("legs").getJSONObject(0)
                        val distanceText = leg.getJSONObject("distance").getString("text")
                        val durationText = leg.getJSONObject("duration").getString("text")
                        val endAddress = leg.getString("end_address")

                        val stepsArray = leg.getJSONArray("steps")
                        for (i in 0 until stepsArray.length()) {
                            val step = stepsArray.getJSONObject(i)
                            val polyline = step.getJSONObject("polyline").getString("points")
                            points.addAll(decodePolyline(polyline))
                        }

                        runOnUiThread {
                            mMap.addPolyline(
                                PolylineOptions()
                                    .addAll(points)
                                    .width(8f)
                                    .color(ContextCompat.getColor(this@MapsActivity, R.color.teal_700))
                            )
                            showRoutePanel(endAddress, durationText, distanceText)
                        }
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

            val latLng = LatLng(lat / 1E5, lng / 1E5)
            poly.add(latLng)
        }

        return poly
    }

    private fun showRoutePanel(destination: String, duration: String, distance: String) {
        Log.d("ROUTE_PANEL", "showRoutePanel called")
        val panel = findViewById<View>(R.id.route_info_panel)
        panel.visibility = View.VISIBLE
        // Скрыть нижнее меню и показать кнопку
        findViewById<ConstraintLayout>(R.id.bottom_menu).visibility = View.GONE
        findViewById<ImageButton>(R.id.btn_show_search).visibility = View.VISIBLE


        findViewById<TextView>(R.id.route_destination_name).text = destination
        findViewById<TextView>(R.id.route_duration).text = "$duration • $distance"
        findViewById<Button>(R.id.btn_open_google_maps).setOnClickListener {
            if (currentLocation != null && destinationMarker != null) {
                val startLat = currentLocation!!.latitude
                val startLng = currentLocation!!.longitude
                val destLat = destinationMarker!!.position.latitude
                val destLng = destinationMarker!!.position.longitude

                val travelMode = when (findViewById<Spinner>(R.id.transport_mode_spinner).selectedItem.toString()) {
                    "Авто" -> "driving"
                    "Пешком" -> "walking"
                    "Общественный транспорт" -> "transit"
                    else -> "driving"
                }

                val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin=$startLat,$startLng&destination=$destLat,$destLng&travelmode=$travelMode")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")

                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Google Maps не установлены", Toast.LENGTH_SHORT).show()
                }
            }
        }
        findViewById<ImageButton>(R.id.btn_show_search).visibility = View.VISIBLE
        findViewById<ConstraintLayout>(R.id.bottom_menu).visibility = View.GONE

    }



    private fun signOut() {
        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        prefs.edit().remove("token").apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
//    private fun setupAutocomplete() {
//        val autocompleteFragment =
//            supportFragmentManager.findFragmentById(R.id.place_autocomplete_fragment) as AutocompleteSupportFragment
//
//        // Устанавливаем, какие данные о месте хотим получить
//        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))
//
//        // Отключаем автоматическое скрытие поисковой строки при вводе
//        autocompleteFragment.view?.findViewById<View>(com.google.android.libraries.places.R.id.places_autocomplete_search_input)?.setOnFocusChangeListener { v, hasFocus ->
//            if (hasFocus) {
//                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
//                imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
//            }
//        }
//
//        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
//            override fun onPlaceSelected(place: Place) {
//                Log.i("MapsActivity", "Выбрано место: ${place.name}, ${place.latLng}")
//
//                place.latLng?.let { latLng ->
//                    mMap.clear()
//                    mMap.addMarker(
//                        MarkerOptions()
//                            .position(latLng)
//                            .title(place.name)
//                    )
//                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
//                }
//            }
//
//            override fun onError(status: com.google.android.gms.common.api.Status) {
//                Log.e("MapsActivity", "Ошибка поиска места: $status")
//            }
//        })
//    }

    companion object {
        private const val LOCATION_REQUEST_CODE = 1
    }



    private fun fetchNearbyPlaces(city: String, onComplete: () -> Unit) {
        val query = "police station in $city OR hospital in $city"
        val url = "https://maps.googleapis.com/maps/api/place/textsearch/json?" +
                "query=${query.replace(" ", "+")}&key=AIzaSyANf9wnCcRFRslApgQTjqYhDLOg6nIQ9-E"

        val request = Request.Builder().url(url).build()

        val policeIcon = BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.ic_police)!!)
        val hospitalIcon = BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.ic_hospital)!!)

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("SafePlaces", "Request failed: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body?.string() ?: return
                val json = JSONObject(body)
                val results = json.getJSONArray("results")

                runOnUiThread {
                    nearbyPlaceMarkers.forEach { it.remove() }
                    nearbyPlaceMarkers.clear()

                    for (i in 0 until results.length()) {
                        val place = results.getJSONObject(i)
                        val name = place.getString("name")
                        val location = place.getJSONObject("geometry").getJSONObject("location")
                        val lat = location.getDouble("lat")
                        val lng = location.getDouble("lng")

                        val icon = getPlaceIcon(name, policeIcon, hospitalIcon)

                        val markerOptions = MarkerOptions()
                            .position(LatLng(lat, lng))
                            .title(name)
                            .icon(icon)

                        val marker = mMap.addMarker(markerOptions)
                        marker?.let { nearbyPlaceMarkers.add(it) }
                    }
                    runOnUiThread { onComplete() }
                }
            }
        })
    }

    private fun getPlaceIcon(name: String, policeIcon: BitmapDescriptor, hospitalIcon: BitmapDescriptor): BitmapDescriptor {
        val policeKeywords = listOf(
            "police", "Police", "POLICE",
            "полиция", "Полиция", "ПОЛИЦИЯ",
            "law enforcement", "Law Enforcement", "LAW ENFORCEMENT",
            "public safety", "Public Safety", "PUBLIC SAFETY",
            "внутренних дел", "Внутренних Дел", "ВНУТРЕННИХ ДЕЛ",
            "отдел полиции", "Отдел Полиции", "ОТДЕЛ ПОЛИЦИИ",
            "департамент полиции", "Департамент Полиции", "ДЕПАРТАМЕНТ ПОЛИЦИИ",
            "управление полиции", "Управление Полиции", "УПРАВЛЕНИЕ ПОЛИЦИИ",
            "патруль", "Патруль", "ПАТРУЛЬ",
            "дежурная часть", "Дежурная Часть", "ДЕЖУРНАЯ ЧАСТЬ",
            "security service", "Security Service", "SECURITY SERVICE",
            "участковый пункт полиции", "Участковый Пункт Полиции", "УЧАСТКОВЫЙ ПУНКТ ПОЛИЦИИ",
            "ГАИ", "Гаи", "гai", "ГAИ",
            "ГОВД", "Govd", "govd", "Говд"
        )

        val hospitalKeywords = listOf(
            "hospital", "больница", "clinic", "клиника", "медцентр", "медицинский",
            "emergency", "ambulance", "амбулатория", "healthcare", "медпункт",
            "травмпункт", "терапия", "surgery", "помощь", "health center",
            "Многопрофильная областная больница города Кызылорда", "Областная Детская Больница",
            "Поликлиника", "поликлиника", "Медицинский центр", "Центр здоровья", "Центр медобслуживания"
        )

        return when {
            policeKeywords.any { name.contains(it) } -> {
                Log.d("SafePlaces", "🛡️ POLICE MATCH: $name")
                policeIcon
            }
            hospitalKeywords.any { name.contains(it) } -> {
                Log.d("SafePlaces", "🏥 HOSPITAL MATCH: $name")
                hospitalIcon
            }
            else -> {
                Log.d("SafePlaces", "❓ UNKNOWN PLACE: $name")
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            }
        }
    }

    private fun fetchNearbyMedicalPlaces(city: String, onComplete: () -> Unit) {
        val query = "больница OR поликлиника OR клиника OR медицинский центр in $city"
        val url = "https://maps.googleapis.com/maps/api/place/textsearch/json?" +
                "query=${query.replace(" ", "+")}&key=AIzaSyANf9wnCcRFRslApgQTjqYhDLOg6nIQ9-E"

        val request = Request.Builder().url(url).build()

        val hospitalIcon = BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.ic_hospital)!!)

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("MedicalPlaces", "Request failed: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body?.string() ?: return
                val json = JSONObject(body)
                val results = json.getJSONArray("results")

                runOnUiThread {
                    for (i in 0 until results.length()) {
                        val place = results.getJSONObject(i)
                        val name = place.getString("name")
                        val location = place.getJSONObject("geometry").getJSONObject("location")
                        val lat = location.getDouble("lat")
                        val lng = location.getDouble("lng")

                        val markerOptions = MarkerOptions()
                            .position(LatLng(lat, lng))
                            .title(name)
                            .icon(hospitalIcon)

                        val marker = mMap.addMarker(markerOptions)
                        marker?.let { nearbyPlaceMarkers.add(it) }
                    }
                    runOnUiThread { onComplete() }
                }
            }
        })
    }

    private fun fetchNearbyPolicePlaces(city: String, onComplete: () -> Unit) {
        val query = "полиция OR отдел полиции OR полиция гаи in $city"
        val url = "https://maps.googleapis.com/maps/api/place/textsearch/json?" +
                "query=${query.replace(" ", "+")}&key=AIzaSyANf9wnCcRFRslApgQTjqYhDLOg6nIQ9-E"

        val request = Request.Builder().url(url).build()

        val policeIcon = BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.ic_police)!!)

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("PolicePlaces", "Request failed: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body?.string() ?: return
                val json = JSONObject(body)
                val results = json.getJSONArray("results")

                runOnUiThread {
                    for (i in 0 until results.length()) {
                        val place = results.getJSONObject(i)
                        val name = place.getString("name")
                        val location = place.getJSONObject("geometry").getJSONObject("location")
                        val lat = location.getDouble("lat")
                        val lng = location.getDouble("lng")

                        val markerOptions = MarkerOptions()
                            .position(LatLng(lat, lng))
                            .title(name)
                            .icon(policeIcon)

                        val marker = mMap.addMarker(markerOptions)
                        marker?.let { nearbyPlaceMarkers.add(it) }
                    }
                    runOnUiThread { onComplete() }
                }
            }
        })
    }

    private val markerPersonMap = mutableMapOf<Marker, DangerousPerson>()

    private fun fetchDangerousPlacesFromApi(onComplete: () -> Unit) {
        val geocoder = Geocoder(this)
        val addresses = try {
            geocoder.getFromLocation(currentLatitude, currentLongitude, 1)
        } catch (e: Exception) {
            Log.e("DangerousPlaces", "Ошибка определения города: ${e.message}")
            Toast.makeText(this, "Ошибка определения города", Toast.LENGTH_SHORT).show()
            null
        }

        val userCity = addresses?.firstOrNull()?.locality ?: addresses?.firstOrNull()?.subAdminArea ?: ""
        if (userCity.isBlank()) return

        val api = RetrofitClient.getInstance(this).create(AuthApi::class.java)
        val token = UserManager.getAuthToken(this) ?: return

        api.getDangerousPlaces("Bearer $token").enqueue(object : Callback<List<DangerousPerson>> {
            override fun onResponse(call: Call<List<DangerousPerson>>, response: Response<List<DangerousPerson>>) {
                val people = response.body()?.filter { it.city.equals(userCity, ignoreCase = true) } ?: emptyList()
                if (people.isEmpty()) return

                lifecycleScope.launch(Dispatchers.IO) {
                    val icon = BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.ic_danger_red)!!)

                    people.forEach { person ->
                        try {
                            val locations = geocoder.getFromLocationName(person.address, 1)
                            if (!locations.isNullOrEmpty()) {
                                val location = locations[0]
                                val latLng = LatLng(location.latitude, location.longitude)

                                withContext(Dispatchers.Main) {
                                    val marker = mMap.addMarker(
                                        MarkerOptions()
                                            .position(latLng)
                                            .title(person.fullName)
                                            .icon(icon)
                                    )
                                    marker?.let {
                                        dangerousMarkers.add(it)
                                        markerPersonMap[it] = person
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("DangerousPlaces", "Ошибка геокодинга: ${e.message}")
                        }
                    }

                    withContext(Dispatchers.Main) {
                        mMap.setOnMarkerClickListener { clickedMarker ->
                            markerPersonMap[clickedMarker]?.let { person ->
                                showDangerousPersonDialog(person)
                                true
                            } ?: false
                        }
                        onComplete()
                    }
                }
            }

            override fun onFailure(call: Call<List<DangerousPerson>>, t: Throwable) {
                Log.e("DangerousPlaces", "Ошибка получения данных: ${t.message}")
            }
        })
    }


    private fun showDangerousPersonDialog(person: DangerousPerson) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_dangerous_person, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.dangerous_person_image)

        Glide.with(this)
            .load(person.photoUrl)
            .placeholder(R.drawable.ic_danger_red)
            .into(imageView)

        AlertDialog.Builder(this)
            .setTitle(person.fullName)
            .setMessage("Адрес: ${person.address}")
            .setView(dialogView)
            .setPositiveButton("Закрыть", null)
            .show()
    }



//    private fun showDangerousPlacesFromAssets(onComplete: () -> Unit) {
//        val geocoder = Geocoder(this)
//        val addresses = try {
//            geocoder.getFromLocation(currentLatitude, currentLongitude, 1)
//        } catch (e: Exception) {
//            Log.e("DangerousPlaces", "Ошибка определения города: ${e.message}")
//            Toast.makeText(this, "Ошибка определения города", Toast.LENGTH_SHORT).show()
//            null
//        }
//
//        val userCity = addresses?.firstOrNull()?.locality ?: addresses?.firstOrNull()?.subAdminArea ?: ""
//        if (userCity.isBlank()) return
//
//        try {
//            val inputStream = assets.open("dangerous_addresses.json")
//            val json = inputStream.bufferedReader().use { it.readText() }
//            val jsonArray = JSONObject("{\"addresses\":$json}").getJSONArray("addresses")
//
//            if (jsonArray.length() == 0) return
//
//            var remaining = 0
//
//            for (i in 0 until jsonArray.length()) {
//                val item = jsonArray.getJSONObject(i)
//                val address = item.getString("address")
//                val fullName = item.getString("fullName")
//
//                // Фильтрация по названию города
//                if (!address.contains(userCity, ignoreCase = true)) continue
//
//                remaining++
//
//                lifecycleScope.launch(Dispatchers.IO) {
//                    try {
//                        val locations = geocoder.getFromLocationName(address, 1)
//                        if (!locations.isNullOrEmpty()) {
//                            val location = locations[0]
//                            val latLng = LatLng(location.latitude, location.longitude)
//
//                            withContext(Dispatchers.Main) {
//                                val icon = BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.ic_danger_red)!!)
//                                val marker = mMap.addMarker(
//                                    MarkerOptions()
//                                        .position(latLng)
//                                        .title(fullName)
//                                        .icon(icon)
//                                )
//                                marker?.let { dangerousMarkers.add(it) }
//                            }
//                        }
//                    } catch (e: Exception) {
//                        Log.e("DangerousPlaces", "Geocoding failed for $address: ${e.message}")
//                    } finally {
//                        withContext(Dispatchers.Main) {
//                            remaining--
//                            if (remaining == 0) {
//                                onComplete()
//                            }
//                        }
//                    }
//                }
//            }
//
//            if (remaining == 0) onComplete()
//        } catch (e: Exception) {
//            Log.e("DangerousPlaces", "Error loading JSON: ${e.message}")
//        }
//    }


}


