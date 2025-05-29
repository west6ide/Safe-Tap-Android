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
) : RecyclerView.Adapter<EmergencyContactAdapter.ContactViewHolder>() {

    var isEditMode = false

    inner class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.contact_avatar)
        val name: TextView = view.findViewById(R.id.contact_name)
        val deleteIcon: ImageButton = view.findViewById(R.id.delete_contact)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emergency_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.name.text = "User ${contact.contactId}" // или подставить имя, если оно есть
        holder.deleteIcon.visibility = if (isEditMode) View.VISIBLE else View.GONE

        holder.deleteIcon.setOnClickListener {
            onDelete(contact)
        }
    }

    override fun getItemCount(): Int = contacts.size
}
