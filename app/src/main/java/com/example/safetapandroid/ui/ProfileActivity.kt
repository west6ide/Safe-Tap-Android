package com.example.safetapandroid.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.safetapandroid.R
import com.example.safetapandroid.network.AuthApi
import com.example.safetapandroid.network.RetrofitClient
import com.example.safetapandroid.utils.UserManager
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.InputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var nameText: TextView
    private lateinit var phoneEdit: EditText
    private lateinit var emailEdit: EditText
    private lateinit var saveButton: Button
    private lateinit var changePhotoButton: ImageButton

    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_activity)

        profileImage = findViewById(R.id.profile_image)
        nameText = findViewById(R.id.profile_name)
        phoneEdit = findViewById(R.id.edit_phone)
        emailEdit = findViewById(R.id.edit_email)
        saveButton = findViewById(R.id.btn_save)
        changePhotoButton = findViewById(R.id.btn_change_photo)

        val name = intent.getStringExtra("name") ?: ""
        val phone = intent.getStringExtra("phone") ?: ""
        val email = intent.getStringExtra("email") ?: ""

        nameText.text = name
        phoneEdit.setText(phone)
        emailEdit.setText(email)

        val backBtn = findViewById<ImageButton>(R.id.back_button)
        val editBtn = findViewById<ImageButton>(R.id.edit_button)

        changePhotoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        backBtn.setOnClickListener {
            onBackPressed()
        }

        editBtn.setOnClickListener {
            Toast.makeText(this, "Edit profile feature coming soon", Toast.LENGTH_SHORT).show()
        }

        loadProfile()

        saveButton.setOnClickListener {
            updateProfile()
        }
    }

    private fun loadProfile() {
        val token = UserManager.getAuthToken(this) ?: return
        val api = RetrofitClient.getInstance(this).create(AuthApi::class.java)

        api.getProfile("Bearer $token").enqueue(object : Callback<com.example.safetapandroid.network.UserProfile> {
            override fun onResponse(
                call: Call<com.example.safetapandroid.network.UserProfile>,
                response: Response<com.example.safetapandroid.network.UserProfile>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!
                    nameText.text = profile.fullName
                    phoneEdit.setText(profile.phoneNumber)
                    emailEdit.setText(profile.email)

                    if (!isFinishing && !isDestroyed) {
                        Glide.with(this@ProfileActivity)
                            .load(R.drawable.ic_profile_placeholder)
                            .into(profileImage)
                    }
                } else {
                    Toast.makeText(this@ProfileActivity, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.example.safetapandroid.network.UserProfile>, t: Throwable) {
                Toast.makeText(this@ProfileActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateProfile() {
        val token = UserManager.getAuthToken(this) ?: return
        val name = nameText.text.toString()
        val phone = phoneEdit.text.toString()
        val email = emailEdit.text.toString()

        val api = RetrofitClient.getInstance(this).create(AuthApi::class.java)
        val body = mutableMapOf(
            "name" to name,
            "phone" to phone,
            "email" to email
        )

        selectedImageUri?.let {
            val avatarBase64 = uriToBase64(it)
            body["avatar"] = avatarBase64
        }

        api.updateProfile("Bearer $token", body).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ProfileActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ProfileActivity, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@ProfileActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            profileImage.setImageURI(selectedImageUri)
        }
    }

    private fun uriToBase64(uri: Uri): String {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }
}
