package com.example.safetapandroid.ui.fakecall

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.safetapandroid.R
import com.example.safetapandroid.network.FakeCall

class FakeCallAdapter(
    private var calls: MutableList<FakeCall>,
    private val onDelete: (FakeCall) -> Unit,
    private val onEdit: (FakeCall) -> Unit
) : RecyclerView.Adapter<FakeCallAdapter.FakeCallViewHolder>() {

    inner class FakeCallViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.name_text)
        val timeText: TextView = view.findViewById(R.id.time_text)
        val deleteButton: ImageButton = view.findViewById(R.id.delete_button)
        val editButton: ImageButton = view.findViewById(R.id.edit_button)

        init {
            deleteButton.setOnClickListener {
                onDelete(calls[adapterPosition])
            }
            editButton.setOnClickListener {
                onEdit(calls[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FakeCallViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fake_call, parent, false)
        return FakeCallViewHolder(view)
    }

    override fun onBindViewHolder(holder: FakeCallViewHolder, position: Int) {
        val call = calls[position]
        holder.nameText.text = call.name
        holder.timeText.text = String.format("%02d:%02d", call.hour, call.minute)
    }

    override fun getItemCount() = calls.size

    fun updateList(newList: MutableList<FakeCall>) {
        calls = newList
        notifyDataSetChanged()
    }
}