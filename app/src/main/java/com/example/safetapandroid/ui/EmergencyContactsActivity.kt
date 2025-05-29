package com.example.safetapandroid.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.safetapandroid.R
import com.example.safetapandroid.network.AuthApi
import com.example.safetapandroid.network.EmergencyContact
import com.example.safetapandroid.network.RetrofitClient
import com.example.safetapandroid.utils.UserManager
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EmergencyContactsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EmergencyContactAdapter
    private val contacts = mutableListOf<EmergencyContact>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_AppCompat_Light_NoActionBar_FullScreen)
        setContentView(R.layout.activity_emergency_contacts)

        recyclerView = findViewById(R.id.recycler_contacts)
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        adapter = EmergencyContactAdapter(contacts, this::deleteContact)
        recyclerView.adapter = adapter

        findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            finish()
        }

        findViewById<ImageButton>(R.id.add_button).setOnClickListener {
            startActivity(Intent(this, AddEmergencyContactActivity::class.java))
        }

        findViewById<ImageButton>(R.id.edit_mode_button).setOnClickListener {
            adapter.isEditMode = !adapter.isEditMode
            adapter.notifyDataSetChanged()
        }

        loadContacts()
    }

    private fun loadContacts() {
        val token = UserManager.getAuthToken(this) ?: return
        RetrofitClient.getInstance(this).create(AuthApi::class.java)
            .getEmergencyContacts("Bearer $token")
            .enqueue(object : Callback<List<EmergencyContact>> {
                override fun onResponse(call: Call<List<EmergencyContact>>, response: Response<List<EmergencyContact>>) {
                    if (response.isSuccessful) {
                        contacts.clear()
                        contacts.addAll(response.body() ?: emptyList())
                        adapter.notifyDataSetChanged()
                        findViewById<TextView>(R.id.contact_count).text = "${contacts.size}/9"
                    }
                }

                override fun onFailure(call: Call<List<EmergencyContact>>, t: Throwable) {
                    Toast.makeText(this@EmergencyContactsActivity, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun deleteContact(contact: EmergencyContact) {
        val token = UserManager.getAuthToken(this) ?: return
        val request = mapOf("phone_number" to contact.phone)
        RetrofitClient.getInstance(this).create(AuthApi::class.java)
            .deleteEmergencyContact("Bearer $token", request)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) loadContacts()
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@EmergencyContactsActivity, "Ошибка удаления", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
