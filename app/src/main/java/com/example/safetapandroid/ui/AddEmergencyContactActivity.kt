package com.example.safetapandroid.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.safetapandroid.R
import com.example.safetapandroid.network.AuthApi
import com.example.safetapandroid.network.AuthApi.AddContactRequest
import com.example.safetapandroid.network.RetrofitClient
import com.example.safetapandroid.utils.UserManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import okhttp3.ResponseBody

class AddEmergencyContactActivity : AppCompatActivity() {

    private lateinit var phoneInput: EditText
    private lateinit var addButton: Button
    private lateinit var api: AuthApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_emergency_contact)
        findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            finish()
        }

        phoneInput = findViewById(R.id.input_phone)
        addButton = findViewById(R.id.btn_add_contact)

        api = RetrofitClient.getInstance(this).create(AuthApi::class.java)

        addButton.setOnClickListener {
            val phone = phoneInput.text.toString().trim()
            if (phone.isNotEmpty()) {
                addEmergencyContact(phone)
            } else {
                Toast.makeText(this, "Введите номер телефона", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addEmergencyContact(phone: String) {
        val token = UserManager.getAuthToken(this) ?: return
        val request = AddContactRequest(phone)

        api.addEmergencyContact("Bearer $token", request)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        runOnUiThread {
                            androidx.appcompat.app.AlertDialog.Builder(this@AddEmergencyContactActivity)
                                .setTitle("Успешно")
                                .setMessage("Контакт добавлен в экстренные.")
                                .setPositiveButton("Ок") { dialog, _ ->
                                    dialog.dismiss()
                                    finish()
                                }
                                .show()
                        }

                    } else {
                        val errorMsg = response.errorBody()?.string() ?: "Ошибка"
                        Toast.makeText(this@AddEmergencyContactActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@AddEmergencyContactActivity, "Ошибка: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
