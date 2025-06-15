package com.example.safetapandroid.network

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header

// Модель запроса на вход
data class LoginRequest(val phone: String, val password: String)

// Модель запроса на регистрацию
data class RegisterRequest(val name: String, val phone: String, val password: String, val confirmPassword: String)

// Модель ответа (с токеном)
data class AuthResponse(
    val token: String?,
    val refreshToken: String?,
    val user_id: Int?
)

data class SOSRequest(
    val latitude: Double,
    val longitude: Double
)

data class Notification(
    val id: Int,
    val userId: Int,
    val contactId: Int,
    val message: String,
    val latitude: Double,
    val longitude: Double,
    val createdAt: String
)
data class LiveLocation(
    val userId: Int,
    val latitude: Double,
    val longitude: Double,
    val updatedAt: String,
    val name: String // 👈 добавляем имя
)

data class EmergencyContact(
    @SerializedName("phone_number")
    val phone: String,

    @SerializedName("contact_id")
    val contactId: Int,

    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("created_at")
    val createdAt: String
)



data class FakeCall(
    val id: Long = System.currentTimeMillis(), // ID нужен для удаления и поиска
    val name: String,
    val number: String,
    val hour: Int,
    val minute: Int,
    val role: String = "assistant" // по умолчанию
)

data class DangerousPerson(
    val id: Int,
    val fullName: String,
    val city: String,
    val address: String,
    val photoUrl: String
)

data class UserProfile(
    @SerializedName("Name") val fullName: String,
    @SerializedName("Phone") val phoneNumber: String,
    @SerializedName("Email") val email: String
)

data class CrimeReport(
    val id: Int,
    val type: String,
    val article: String,
    val severity: String,
    val region: String,
    val street: String,
    val house: String,
    val place_type: String,
    val target: String,
    val department: String,
    val crime_date: String,
    val kusi_number: String,
    val latitude: Double,
    val longitude: Double,
    val createdAt: String
)

data class SharedRoute(
    val id: Int = 0,
    val senderId: Int, // ✅ правильно
    val startLat: Double,
    val startLng: Double,
    val destLat: Double,
    val destLng: Double,
    val duration: String,
    val distance: String,
    val createdAt: String
)





interface AuthApi {
    @POST("/login")
    fun login(@Body request: LoginRequest): Call<AuthResponse>

    @POST("/register")
    fun register(@Body request: RegisterRequest): Call<AuthResponse>

    @POST("/sos")
    fun sendSOS(@Header("Authorization") token: String, @Body request: SOSRequest): Call<ResponseBody>


    @GET("/notifications")
    fun getNotifications(@Header("Authorization") token: String): Call<List<Notification>>


    @GET("/getUserId")
    fun getUserId(@Header("Authorization") authHeader: String): Call<AuthResponse>

    @GET("/refreshToken")
    fun refreshToken(@Header("Authorization") authHeader: String): Call<AuthResponse>


    @GET("/location/emergency")
    fun getEmergencyLocations(
        @Header("Authorization") token: String
    ): Call<List<LiveLocation>>

    @POST("/location/update")
    fun updateLocation(
        @Header("Authorization") token: String,
        @Body request: SOSRequest
    ): Call<ResponseBody>

    @GET("/dangerous-people")
    fun getDangerousPlaces(
        @Header("Authorization") token: String
    ): Call<List<DangerousPerson>>

    @POST("/contacts/add")
    fun addEmergencyContact(
        @Header("Authorization") token: String,
        @Body contact: AddContactRequest
    ): Call<ResponseBody>

    data class AddContactRequest(
        @SerializedName("phone_number") val phoneNumber: String,
    )

    @GET("/contacts")
    fun getEmergencyContacts(@Header("Authorization") token: String): Call<List<EmergencyContact>>

    @POST("/contacts/delete")
    fun deleteEmergencyContact(
        @Header("Authorization") token: String,
        @Body request: Map<String, String>
    ): Call<ResponseBody>


    @GET("/profile")
    fun getProfile(@Header("Authorization") token: String): Call<UserProfile>


    @POST("/profile/update")
    fun updateProfile(
        @Header("Authorization") token: String,
        @Body profile: Map<String, String>
    ): Call<ResponseBody>


    @GET("/crimes/get")
    fun getCrimeReports(): Call<List<CrimeReport>>

    @POST("/share_route")
    fun sendRouteToContacts(
        @Header("Authorization") token: String,
        @Body route: SharedRoute
    ): Call<ResponseBody>

    @GET("/shared_routes")
    fun getSharedRoutes(
        @Header("Authorization") token: String
    ): Call<List<SharedRoute>>




//    @POST("fake-call")
//    suspend fun scheduleCall(
//        @Body call: FakeCall,
//        @Header("Authorization") token: String
//    ): Response<Map<String, String>>
//
//    @GET("fake-call")
//    suspend fun getUserCalls(
//        @Header("Authorization") token: String
//    ): Response<List<FakeCall>>
//
//    @DELETE("fake-call")
//    suspend fun deleteCall(
//        @Query("id") id: Int,
//        @Header("Authorization") token: String
//    ): Response<Map<String, String>>



//    @POST("/login/google")
//    fun googleLogin(@Body request: GoogleSignInRequest): Call<AuthResponse>

}
