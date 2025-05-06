package com.example.safetapandroid.network

import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Call
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

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
