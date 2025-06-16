package com.example.safetapandroid.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.AsyncTask
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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.safetapandroid.network.CrimeReport
import com.example.safetapandroid.network.DangerousPerson
import com.example.safetapandroid.network.SharedRoute
import com.example.safetapandroid.network.UserProfile
import com.example.safetapandroid.ui.fakecall.FakeCallSchedulerActivity
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import okhttp3.ResponseBody
import java.net.HttpURLConnection
import java.net.URL


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentMarker: Marker? = null
    private val client = OkHttpClient()
    private lateinit var placesClient: PlacesClient
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



    private lateinit var drawerLayout: DrawerLayout
    private lateinit var profileName: TextView
    private lateinit var profilePhone: TextView
    private lateinit var profileEmail: TextView

    data class NavigationStep(val instruction: String, val location: LatLng)

    private val navigationSteps = mutableListOf<NavigationStep>()
    private var currentStepIndex = 0

    private var currentRoutePolyline: Polyline? = null
    private var isNavigationStarted = false
    private var drawnPolyline: Polyline? = null

    private lateinit var autocompleteFragment: AutocompleteSupportFragment

    private var isCrimesVisible = false
    private val crimeMarkers = mutableListOf<Marker>()

    //Dangerous persons and places
    private val categoryCrimeMarkers = mutableListOf<Marker>()
    private var isCategoryVisible = false


    private var currentRouteDistance: String = ""
    private var currentRouteDuration: String = ""





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        findViewById<ImageButton>(R.id.btn_show_search).visibility = View.GONE



        // 1. DrawerLayout (родительский layout)
        drawerLayout = findViewById(R.id.drawer_layout)

        // 2. Получаем View навигационного drawer'а
        val navDrawer: View = findViewById<DrawerLayout>(R.id.nav_drawer)

        val profileImage = navDrawer.findViewById<ImageView>(R.id.profile_image)

        profileImage.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }


        profileName = navDrawer.findViewById(R.id.profile_name)
        profilePhone = navDrawer.findViewById(R.id.profile_phone)
        profileEmail = navDrawer.findViewById(R.id.profile_email)
        loadUserProfile()



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
        findViewById<LinearLayout>(R.id.location_access_option).setOnClickListener {
            val intent = Intent(this, EmergencyContactsActivity::class.java)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        val btnOptions = findViewById<ImageButton>(R.id.btn_options)


        //Dangerous persons and places
        val panelDangerMain = findViewById<LinearLayout>(R.id.panel_danger_main)
        val panelCategoriesScroll = findViewById<ScrollView>(R.id.panel_danger_categories)
        val panelCategoriesLayout = findViewById<LinearLayout>(R.id.layout_danger_categories) // внутренний layout!

        val btnDangerPlaces = findViewById<LinearLayout>(R.id.btn_danger_places)
        val btnDangerPersons = findViewById<LinearLayout>(R.id.btn_danger_persons)

        val btnCategoryRobbery = findViewById<LinearLayout>(R.id.btn_category_robbery)
        val btnCategoryViolence = findViewById<LinearLayout>(R.id.btn_category_violence)
        val btnCategoryMurder = findViewById<LinearLayout>(R.id.btn_category_murder)

        btnOptions.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        findViewById<ImageButton>(R.id.btn_emergency_contacts).setOnClickListener {
            panelDangerMain.visibility = View.VISIBLE
            panelCategoriesScroll.visibility = View.GONE
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


        // Получаем координаты из уведомления
        sosLatitude = intent.getDoubleExtra("notification_latitude", 0.0)
        sosLongitude = intent.getDoubleExtra("notification_longitude", 0.0)

        isFromNotification = sosLatitude != 0.0 && sosLongitude != 0.0

        val geocoder = Geocoder(this)
        val addresses = geocoder.getFromLocation(currentLatitude, currentLongitude, 1)
        val cityName = addresses?.firstOrNull()?.locality ?: ""


// Настраиваем Spinner
        setupAutocompleteSearch(cityName)

        findViewById<Button>(R.id.btn_navigate).setOnClickListener {
            findViewById<View>(R.id.route_info_panel).visibility = View.GONE
            findViewById<View>(R.id.navigation_panel).visibility = View.VISIBLE
            findViewById<ConstraintLayout>(R.id.bottom_menu).visibility = View.GONE

            // Показать пошаговые инструкции
            val instructions = navigationSteps.map { it.instruction }

            val recycler = findViewById<RecyclerView>(R.id.recycler_step_instructions)
            recycler.layoutManager = LinearLayoutManager(this)
            recycler.adapter = InstructionAdapter(instructions)
        }


        findViewById<Button>(R.id.btn_cancel_route).setOnClickListener {
            findViewById<View>(R.id.route_info_panel).visibility = View.GONE
            findViewById<ConstraintLayout>(R.id.bottom_menu).visibility = View.VISIBLE
            drawnPolyline?.remove()
            currentRoutePolyline?.remove()
            destinationMarker?.remove()
            drawnPolyline = null
            destinationMarker = null
            navigationSteps.clear()
        }

        findViewById<Button>(R.id.btn_cancel_navigation).setOnClickListener {
            findViewById<View>(R.id.navigation_panel).visibility = View.GONE
            findViewById<ConstraintLayout>(R.id.bottom_menu).visibility = View.VISIBLE
            drawnPolyline?.remove()
            currentRoutePolyline?.remove()
            destinationMarker?.remove()
            drawnPolyline = null
            destinationMarker = null
            navigationSteps.clear()
            navigationSteps.clear()
        }




// выбор "Places"
        btnDangerPlaces.setOnClickListener {
            panelDangerMain.visibility = View.GONE
            panelCategoriesScroll.visibility = View.VISIBLE
        }

// выбор "Persons"
        btnDangerPersons.setOnClickListener {
            if (isDangerousPlacesVisible) {
                dangerousMarkers.forEach { it.remove() }
                dangerousMarkers.clear()
                isDangerousPlacesVisible = false
            } else {
                fetchDangerousPlacesFromApi {
                    isDangerousPlacesVisible = true
                }
            }
            panelDangerMain.visibility = View.GONE
            panelCategoriesScroll.visibility = View.GONE

        }

// обработка категорий
        btnCategoryRobbery.setOnClickListener {
            showCrimeMarkersByCategory("Кража")
            panelCategoriesScroll.visibility = View.GONE
        }

        btnCategoryViolence.setOnClickListener {
            showCrimeMarkersByCategory("Насилие")
            panelCategoriesScroll.visibility = View.GONE
        }

        btnCategoryMurder.setOnClickListener {
            showCrimeMarkersByCategory("Убийство")
            panelCategoriesScroll.visibility = View.GONE
        }





        val autocompleteFragment = supportFragmentManager
            .findFragmentById(R.id.place_autocomplete_fragment) as AutocompleteSupportFragment

        autocompleteFragment.setPlaceFields(
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
        )

        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                val destinationLatLng = place.latLng
                if (destinationLatLng != null) {
                    destinationMarker?.remove()
                    destinationMarker = mMap.addMarker(
                        MarkerOptions().position(destinationLatLng).title(place.name)
                    )
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(destinationLatLng, 15f))

                    if (currentLocation != null) {
                        drawRoute(currentLocation!!, destinationLatLng, getSelectedTravelMode())
                    }

                    findViewById<View>(R.id.route_info_panel).visibility = View.VISIBLE
                    findViewById<ConstraintLayout>(R.id.bottom_menu).visibility = View.GONE
                }
            }

            override fun onError(status: Status) {
                Toast.makeText(this@MapsActivity, "Ошибка поиска: $status", Toast.LENGTH_SHORT).show()
            }
        })



        findViewById<Spinner>(R.id.transport_mode_spinner).onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val destination = destinationMarker?.position
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }


        if (intent.getBooleanExtra("show_route_in_panel", false)) {
            val startLat = intent.getDoubleExtra("start_lat", 0.0)
            val startLng = intent.getDoubleExtra("start_lng", 0.0)
            val destLat = intent.getDoubleExtra("dest_lat", 0.0)
            val destLng = intent.getDoubleExtra("dest_lng", 0.0)
            val duration = intent.getStringExtra("duration") ?: ""
            val distance = intent.getStringExtra("distance") ?: ""

            if (startLat != 0.0 && startLng != 0.0 && destLat != 0.0 && destLng != 0.0) {
                showRouteInPanel(startLat, startLng, destLat, destLng, duration, distance)
            }
        }


    }



    private fun loadDangerousPersons() {
        fetchDangerousPlacesFromApi {
            isDangerousPlacesVisible = true
        }
    }


    fun showCrimeMarkersByCategory(type: String) {
        if (isCategoryVisible) {
            // Скрываем маркеры
            categoryCrimeMarkers.forEach { it.remove() }
            categoryCrimeMarkers.clear()
            isCategoryVisible = false
            return
        }

        val apiService = RetrofitClient.getInstance(this).create(AuthApi::class.java)
        apiService.getCrimeReports().enqueue(object : Callback<List<CrimeReport>> {
            override fun onResponse(call: Call<List<CrimeReport>>, response: Response<List<CrimeReport>>) {
                if (response.isSuccessful) {
                    val filtered = response.body()?.filter {
                        it.type.contains(type, ignoreCase = true)
                    } ?: return

                    for (crime in filtered) {
                        val latLng = LatLng(crime.latitude, crime.longitude)
                        val marker = mMap.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title(crime.type)
                                .snippet("${crime.street} ${crime.house}")
                                .icon(getCrimeIcon(type))
                        )
                        marker?.let { categoryCrimeMarkers.add(it) }
                    }
                    isCategoryVisible = true
                }
            }

            override fun onFailure(call: Call<List<CrimeReport>>, t: Throwable) {
                Toast.makeText(this@MapsActivity, "Ошибка загрузки преступлений", Toast.LENGTH_SHORT).show()
            }
        })
    }



    private fun getCrimeIcon(type: String): BitmapDescriptor {
        return when {
            type.contains("Кража", ignoreCase = true) -> {
                BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.ic_robbery)!!)
            }
            type.contains("Насилие", ignoreCase = true) -> {
                BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.ic_violence)!!)
            }
            type.contains("Убийства", ignoreCase = true) -> {
                BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.ic_murder)!!)
            }
            else -> {
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            }
        }
    }




    private fun setupAutocompleteSearch() {
        autocompleteFragment = supportFragmentManager
            .findFragmentById(R.id.place_autocomplete_fragment) as AutocompleteSupportFragment

        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                val destination = place.latLng
                if (destination != null) {
                    drawRouteToDestination(destination)
                    showRoutePanel()
                }
            }

            override fun onError(status: Status) {
                Toast.makeText(this@MapsActivity, "Ошибка: $status", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private val markerCrimeMap = mutableMapOf<Marker, CrimeReport>()

    private fun fetchCrimeReports(onComplete: () -> Unit) {
        val geocoder = Geocoder(this)
        val addresses = try {
            geocoder.getFromLocation(currentLatitude, currentLongitude, 1)
        } catch (e: Exception) {
            Log.e("CrimePlaces", "Ошибка определения города: ${e.message}")
            Toast.makeText(this, "Ошибка определения города", Toast.LENGTH_SHORT).show()
            null
        }

        val userCity = addresses?.firstOrNull()?.locality ?: addresses?.firstOrNull()?.subAdminArea ?: ""
        if (userCity.isBlank()) return

        val api = RetrofitClient.getInstance(this).create(AuthApi::class.java)
        val icon = BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.ic_danger_red)!!)

        api.getCrimeReports().enqueue(object : Callback<List<CrimeReport>> {
            override fun onResponse(call: Call<List<CrimeReport>>, response: Response<List<CrimeReport>>) {
                if (!response.isSuccessful || response.body() == null) {
                    Log.e("CrimePlaces", "Ошибка загрузки преступлений: ${response.code()}")
                    return
                }

                val filteredCrimes = response.body()!!
                    .filter { it.region.contains(userCity, ignoreCase = true) }

                if (filteredCrimes.isEmpty()) {
                    Log.d("CrimePlaces", "Нет преступлений для города: $userCity")
                    return
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    for (crime in filteredCrimes) {
                        try {
                            val addressText = "${crime.street} ${crime.house}, ${crime.region}"
                            val locations = geocoder.getFromLocationName(addressText, 1)

                            if (!locations.isNullOrEmpty()) {
                                val location = locations[0]
                                val latLng = LatLng(location.latitude, location.longitude)

                                withContext(Dispatchers.Main) {
                                    val marker = mMap.addMarker(
                                        MarkerOptions()
                                            .position(latLng)
                                            .title("${crime.type} (${crime.severity})")
                                            .snippet("Улица: ${crime.street} ${crime.house}")
                                            .icon(icon)
                                    )
                                    marker?.let {
                                        crimeMarkers.add(it)
                                        markerCrimeMap[it] = crime
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("CrimePlaces", "Ошибка геокодинга: ${e.message}")
                        }
                    }

                    withContext(Dispatchers.Main) {
                        mMap.setOnInfoWindowClickListener { marker ->
                            markerCrimeMap[marker]?.let { crime ->
                                AlertDialog.Builder(this@MapsActivity)
                                    .setTitle("Детали преступления")
                                    .setMessage(
                                        "Тип: ${crime.type}\n" +
                                                "Статья: ${crime.article}\n" +
                                                "Регион: ${crime.region}\n" +
                                                "Дата: ${crime.crime_date}"
                                    )
                                    .setPositiveButton("Закрыть", null)
                                    .show()
                                true
                            } ?: false
                        }
                        onComplete()
                    }
                }
            }

            override fun onFailure(call: Call<List<CrimeReport>>, t: Throwable) {
                Log.e("CrimePlaces", "Ошибка загрузки: ${t.message}")
                Toast.makeText(this@MapsActivity, "Ошибка загрузки преступлений", Toast.LENGTH_SHORT).show()
            }
        })
    }





    private fun showRoutePanel() {
        findViewById<View>(R.id.route_info_panel).visibility = View.VISIBLE
        findViewById<View>(R.id.navigation_panel).visibility = View.GONE
        findViewById<View>(R.id.bottom_menu).visibility = View.GONE
    }



    private fun drawRouteToDestination(destination: LatLng) {
        if (::mMap.isInitialized) {
            destinationMarker?.remove()
            destinationMarker = mMap.addMarker(MarkerOptions().position(destination).title("Пункт назначения"))

            val url = getDirectionsUrl(LatLng(currentLatitude, currentLongitude), destination)
            FetchURL(this).execute(url)
        }
    }
    private fun getDirectionsUrl(origin: LatLng, dest: LatLng): String {
        val strOrigin = "origin=${origin.latitude},${origin.longitude}"
        val strDest = "destination=${dest.latitude},${dest.longitude}"
        val sensor = "sensor=false"
        val mode = "mode=walking" // Или driving

        val parameters = "$strOrigin&$strDest&$sensor&$mode"
        val output = "json"

        return "https://maps.googleapis.com/maps/api/directions/$output?$parameters&key=YOUR_API_KEY"
    }
    class FetchURL(val context: MapsActivity) : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg params: String?): String {
            val url = URL(params[0])
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()
            val inputStream = connection.inputStream
            val reader = inputStream.bufferedReader()
            return reader.readText()
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            val parserTask = ParserTask(context)
            parserTask.execute(result)
        }
    }


    class ParserTask(private val context: MapsActivity) : AsyncTask<String, Int, List<List<HashMap<String, String>>>>() {

        override fun doInBackground(vararg jsonData: String): List<List<HashMap<String, String>>> {
            val jObject: JSONObject = JSONObject(jsonData[0])
            val parser = DirectionsJSONParser()
            return parser.parse(jObject)
        }

        override fun onPostExecute(result: List<List<HashMap<String, String>>>) {
            val polylineOptions = PolylineOptions()

            for (path in result) {
                val points = mutableListOf<LatLng>()
                for (point in path) {
                    val lat = point["lat"]!!.toDouble()
                    val lng = point["lng"]!!.toDouble()
                    points.add(LatLng(lat, lng))
                }
                polylineOptions.addAll(points)
                polylineOptions.width(10f)
                polylineOptions.color(Color.BLUE)
            }

            context.drawnPolyline?.remove()
            context.drawnPolyline = context.mMap.addPolyline(polylineOptions)
        }
    }


    class DirectionsJSONParser {

        fun parse(jObject: JSONObject): List<List<HashMap<String, String>>> {
            val routes = mutableListOf<List<HashMap<String, String>>>()
            val jRoutes = jObject.getJSONArray("routes")

            for (i in 0 until jRoutes.length()) {
                val path = mutableListOf<HashMap<String, String>>()
                val jLegs = jRoutes.getJSONObject(i).getJSONArray("legs")

                for (j in 0 until jLegs.length()) {
                    val jSteps = jLegs.getJSONObject(j).getJSONArray("steps")

                    for (k in 0 until jSteps.length()) {
                        val polyline = jSteps.getJSONObject(k).getJSONObject("polyline").getString("points")
                        val list = decodePoly(polyline)

                        for (l in list.indices) {
                            val hm = HashMap<String, String>()
                            hm["lat"] = list[l].latitude.toString()
                            hm["lng"] = list[l].longitude.toString()
                            path.add(hm)
                        }
                    }
                }
                routes.add(path)
            }
            return routes
        }

        private fun decodePoly(encoded: String): List<LatLng> {
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
                    result = result or (b and 0x1f shl shift)
                    shift += 5
                } while (b >= 0x20)
                val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
                lat += dlat

                shift = 0
                result = 0
                do {
                    b = encoded[index++].code - 63
                    result = result or (b and 0x1f shl shift)
                    shift += 5
                } while (b >= 0x20)
                val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
                lng += dlng

                val p = LatLng((lat.toDouble() / 1E5), (lng.toDouble() / 1E5))
                poly.add(p)
            }

            return poly
        }
    }



    private fun loadUserProfile() {
        val token = UserManager.getAuthToken(this) ?: return

        val api = RetrofitClient.getInstance(this).create(AuthApi::class.java)
        api.getProfile("Bearer $token").enqueue(object : retrofit2.Callback<UserProfile> {
            override fun onResponse(call: Call<UserProfile>, response: Response<UserProfile>) {
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    findViewById<TextView>(R.id.profile_name).text = user.fullName
                    findViewById<TextView>(R.id.profile_phone).text = user.phoneNumber
                    findViewById<TextView>(R.id.profile_email).text = user.email
                } else {
                    Log.e("API", "Profile load failed: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<UserProfile>, t: Throwable) {
                Log.e("API", "Error loading profile", t)
            }
        })
    }


    private fun getSelectedTravelMode(): String {
        val spinner = findViewById<Spinner>(R.id.transport_mode_spinner)
        return when (spinner.selectedItem.toString()) {
            "Пешком" -> "walking"
            "Общественный транспорт" -> "transit"
            else -> "driving"
        }
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
                drawRoute(currentLocation!!, destination, getSelectedTravelMode())
            }

        }


        startEmergencyLocationUpdates()
        fetchCrimeReports { isCrimesVisible = true }

//        if (intent.getBooleanExtra("show_route", false)) {
//            val startLat = intent.getDoubleExtra("start_lat", 0.0)
//            val startLng = intent.getDoubleExtra("start_lng", 0.0)
//            val destLat = intent.getDoubleExtra("dest_lat", 0.0)
//            val destLng = intent.getDoubleExtra("dest_lng", 0.0)
//
//            if (startLat != 0.0 && startLng != 0.0 && destLat != 0.0 && destLng != 0.0) {
//                val start = LatLng(startLat, startLng)
//                val end = LatLng(destLat, destLng)
//
//                // Добавляем маркеры
//                mMap.addMarker(MarkerOptions().position(start).title("Start"))
//                mMap.addMarker(MarkerOptions().position(end).title("End"))
//
//                // Рисуем маршрут
//                drawRoute(Location("").apply {
//                    latitude = startLat
//                    longitude = startLng
//                }, end, "driving")
//
//                // Центрируем карту
//                val bounds = LatLngBounds.Builder()
//                    .include(start)
//                    .include(end)
//                    .build()
//                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
//            }
//        }
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
        if (isNavigationStarted) {
            checkNavigationProgress(latLng)
        }
    }

    private fun checkNavigationProgress(current: LatLng) {
        if (currentStepIndex >= navigationSteps.size) return

        val targetStep = navigationSteps[currentStepIndex]
        val distance = FloatArray(1)
        Location.distanceBetween(
            current.latitude, current.longitude,
            targetStep.location.latitude, targetStep.location.longitude,
            distance
        )

        if (distance[0] < 30) { // 30 метров — можно уменьшить
            Toast.makeText(this, targetStep.instruction, Toast.LENGTH_LONG).show()
            currentStepIndex++

            if (currentStepIndex == navigationSteps.size) {
                Toast.makeText(this, "Вы прибыли в пункт назначения!", Toast.LENGTH_LONG).show()
            }
        }
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
                        val instructions = mutableListOf<String>() // ⬅️ добавлено
                        val leg = routes.getJSONObject(0).getJSONArray("legs").getJSONObject(0)
                        currentRouteDistance = leg.getJSONObject("distance").getString("text")
                        currentRouteDuration = leg.getJSONObject("duration").getString("text")
                        val endAddress = leg.getString("end_address")

                        navigationSteps.clear()
                        currentStepIndex = 0

                        val stepsArray = leg.getJSONArray("steps")
                        for (i in 0 until stepsArray.length()) {
                            val step = stepsArray.getJSONObject(i)
                            val polyline = step.getJSONObject("polyline").getString("points")
                            points.addAll(decodePolyline(polyline))

                            val htmlInstruction = step.getString("html_instructions")
                            val plainText = HtmlCompat.fromHtml(
                                htmlInstruction,
                                HtmlCompat.FROM_HTML_MODE_LEGACY
                            ).toString()
                            instructions.add(plainText) // ⬅️ добавлено
                            val endLocation = step.getJSONObject("end_location")
                            val lat = endLocation.getDouble("lat")
                            val lng = endLocation.getDouble("lng")
                            navigationSteps += NavigationStep(plainText, LatLng(lat, lng))

                        }

                        runOnUiThread {
                            currentRoutePolyline?.remove()

                            currentRoutePolyline = mMap.addPolyline(
                                PolylineOptions()
                                    .addAll(points)
                                    .width(8f)
                                    .color(ContextCompat.getColor(this@MapsActivity, R.color.teal_700))
                            )
                            showRoutePanel(endAddress, currentRouteDuration, currentRouteDistance) // ⬅️ новый аргумент
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
        val panel = findViewById<View>(R.id.route_info_panel)
        panel.visibility = View.VISIBLE
        findViewById<ConstraintLayout>(R.id.bottom_menu).visibility = View.GONE
        findViewById<ImageButton>(R.id.btn_show_search).visibility = View.VISIBLE

        val modeText = when (findViewById<Spinner>(R.id.transport_mode_spinner).selectedItem.toString()) {
            "Пешком" -> "Пешком"
            "Общественный транспорт" -> "Общественный транспорт"
            else -> "Автомобиль"
        }

        findViewById<TextView>(R.id.route_mode_title).text = modeText
        findViewById<TextView>(R.id.route_time_distance).text = "$duration ($distance)"
        findViewById<TextView>(R.id.route_additional_info).text = "Самый быстрый маршрут с учетом пробок"


        // ✅ Отображаем пошаговые инструкции внутри RecyclerView

        // Кнопка "Поделиться маршрутом"
        findViewById<Button>(R.id.btn_share_route).setOnClickListener {
            UserManager.getUserId(this) { senderId ->
                if (senderId == null) {
                    Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
                    return@getUserId
                }

                val destLat = destinationMarker?.position?.latitude ?: return@getUserId
                val destLng = destinationMarker?.position?.longitude ?: return@getUserId

                val route = SharedRoute(
                    senderId = senderId,
                    startLat = currentLatitude,
                    startLng = currentLongitude,
                    destLat = destLat,
                    destLng = destLng,
                    duration = currentRouteDuration,
                    distance = currentRouteDistance,
                    createdAt = ""
                )

                val api = RetrofitClient.getInstance(this).create(AuthApi::class.java)
                val token = UserManager.getAuthToken(this) ?: return@getUserId

                api.sendRouteToContacts("Bearer $token", route).enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        Toast.makeText(this@MapsActivity, "Маршрут отправлен!", Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Toast.makeText(this@MapsActivity, "Ошибка отправки маршрута", Toast.LENGTH_SHORT).show()
                    }
                })
            }


        }



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




    private fun showRoutePreview(startLat: Double, startLng: Double,
                                 destLat: Double, destLng: Double,
                                 duration: String, distance: String) {

        // Показываем панель
        findViewById<ConstraintLayout>(R.id.navigation_contact_panel).visibility = View.VISIBLE

        // Устанавливаем информацию о маршруте
        findViewById<TextView>(R.id.route_distance).text = distance
        findViewById<TextView>(R.id.route_duration).text = duration

        // Инициализируем карту в контейнере
        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_container, mapFragment)
            .commit()

        mapFragment.getMapAsync { googleMap ->
            val start = LatLng(startLat, startLng)
            val end = LatLng(destLat, destLng)

            // Добавляем маркеры
            googleMap.addMarker(MarkerOptions().position(start).title("Start"))
            googleMap.addMarker(MarkerOptions().position(end).title("End"))

            // Рисуем маршрут
            drawRouteOnMap(googleMap, start, end)

            // Центрируем карту
            val bounds = LatLngBounds.Builder()
                .include(start)
                .include(end)
                .build()
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        }

        // Обработчик кнопки закрытия
        findViewById<Button>(R.id.btn_close_route).setOnClickListener {
            findViewById<ConstraintLayout>(R.id.navigation_contact_panel).visibility = View.GONE
        }
    }

    // Fix the OkHttp callback implementation
    private fun drawRouteOnMap(googleMap: GoogleMap, origin: LatLng, destination: LatLng) {
        val url = getDirectionsUrl(origin, destination)

        val httpClient = OkHttpClient()
        val request = Request.Builder().url(url).build()

        httpClient.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MapsActivity, "Failed to load route", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                try {
                    val jsonData = response.body?.string() ?: return
                    val result = JSONObject(jsonData)
                    val routes = result.getJSONArray("routes")

                    if (routes.length() > 0) {
                        val route = routes.getJSONObject(0)
                        val polyline = route.getJSONObject("overview_polyline").getString("points")
                        val decodedPath = decodePoly(polyline)

                        runOnUiThread {
                            googleMap.addPolyline(
                                PolylineOptions()
                                    .addAll(decodedPath)
                                    .width(10f)
                                    .color(Color.BLUE)
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("RoutePreview", "Error parsing route", e)
                }
            }
        })
    }

    private fun decodePoly(encoded: String): List<LatLng> {
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
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            poly.add(LatLng(lat / 1E5, lng / 1E5))
        }
        return poly
    }



    private fun showRouteInPanel(
        startLat: Double,
        startLng: Double,
        destLat: Double,
        destLng: Double,
        duration: String,
        distance: String
    ) {
        // Show the route preview panel
        val panel = findViewById<ConstraintLayout>(R.id.navigation_contact_panel)
        panel.visibility = View.VISIBLE

        // Set route info
        findViewById<TextView>(R.id.route_distance).text = distance
        findViewById<TextView>(R.id.route_duration).text = duration

        // Initialize map
        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_container, mapFragment)
            .commit()

        mapFragment.getMapAsync { googleMap ->
            val start = LatLng(startLat, startLng)
            val end = LatLng(destLat, destLng)

            // Add markers
            googleMap.addMarker(MarkerOptions().position(start).title("Start"))
            googleMap.addMarker(MarkerOptions().position(end).title("End"))

            // Center camera on route
            val bounds = LatLngBounds.Builder()
                .include(start)
                .include(end)
                .build()
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))

            // Draw route
            drawRouteOnPanelMap(googleMap, start, end)
        }

        // Handle close button
        findViewById<Button>(R.id.btn_close_route).setOnClickListener {
            panel.visibility = View.GONE
        }
    }

    private fun drawRouteOnPanelMap(googleMap: GoogleMap, start: LatLng, end: LatLng) {
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${start.latitude},${start.longitude}" +
                "&destination=${end.latitude},${end.longitude}" +
                "&mode=driving" +
                "&key=AIzaSyANf9wnCcRFRslApgQTjqYhDLOg6nIQ9-E"

        val request = Request.Builder().url(url).build()

        OkHttpClient().newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MapsActivity, "Ошибка загрузки маршрута: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                try {
                    val jsonData = response.body?.string() ?: return
                    Log.d("RouteResponse", jsonData) // Логируем ответ для отладки

                    val jsonObject = JSONObject(jsonData)
                    if (jsonObject.getString("status") != "OK") {
                        runOnUiThread {
                            Toast.makeText(this@MapsActivity, "Ошибка построения маршрута", Toast.LENGTH_LONG).show()
                        }
                        return
                    }

                    val routes = jsonObject.getJSONArray("routes")
                    if (routes.length() == 0) return

                    val route = routes.getJSONObject(0)
                    val legs = route.getJSONArray("legs")
                    if (legs.length() == 0) return

                    // Получаем время и расстояние
                    val leg = legs.getJSONObject(0)
                    val distance = leg.getJSONObject("distance").getString("text")
                    val duration = leg.getJSONObject("duration").getString("text")

                    // Получаем полилинию
                    val overviewPolyline = route.getJSONObject("overview_polyline")
                    val points = overviewPolyline.getString("points")
                    val decodedPath = decodePolylines(points)

                    runOnUiThread {
                        // Обновляем UI с данными о маршруте
                        findViewById<TextView>(R.id.route_distance).text = distance
                        findViewById<TextView>(R.id.route_duration).text = duration

                        // Рисуем маршрут на карте
                        googleMap.addPolyline(
                            PolylineOptions()
                                .addAll(decodedPath)
                                .width(10f)
                                .color(ContextCompat.getColor(this@MapsActivity, R.color.teal_700))
                        )
                    }
                } catch (e: Exception) {
                    Log.e("RouteError", "Ошибка разбора маршрута", e)
                    runOnUiThread {
                        Toast.makeText(this@MapsActivity, "Ошибка обработки маршрута", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun decodePolylines(encoded: String): List<LatLng> {
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
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            poly.add(LatLng(lat / 1E5, lng / 1E5))
        }
        return poly
    }


}


