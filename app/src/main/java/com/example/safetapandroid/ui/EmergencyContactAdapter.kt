package com.example.safetapandroid.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.safetapandroid.R
import com.example.safetapandroid.network.EmergencyContact

class EmergencyContactAdapter(
    private val contacts: List<EmergencyContact>,
    private val onDelete: (EmergencyContact) -> Unit
) : RecyclerView.Adapter<EmergencyContactAdapter.ViewHolder>() {

    var isEditMode = false

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageProfile: ImageView = view.findViewById(R.id.image_profile)
        val textName: TextView = view.findViewById(R.id.text_name)
        val deleteButton: ImageButton = view.findViewById(R.id.delete_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact_tile, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        holder.textName.text = contact.name // или имя, если появится

        // Временное изображение
        holder.imageProfile.setImageResource(R.drawable.ic_profile_placeholder)

        // Показ/скрытие кнопки удаления
        holder.deleteButton.visibility = if (isEditMode) View.VISIBLE else View.GONE

        // Удаление
        holder.deleteButton.setOnClickListener {
            onDelete(contact)
        }
    }

    override fun getItemCount(): Int = contacts.size
}

