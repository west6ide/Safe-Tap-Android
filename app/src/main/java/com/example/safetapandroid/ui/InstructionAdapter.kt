package com.example.safetapandroid.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.safetapandroid.R

class InstructionAdapter(private val steps: List<String>) :
    RecyclerView.Adapter<InstructionAdapter.StepViewHolder>() {

    class StepViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val stepNumber: TextView = view.findViewById(R.id.step_number)
        val stepIcon: ImageView = view.findViewById(R.id.step_icon)
        val stepText: TextView = view.findViewById(R.id.step_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_step_instruction, parent, false)
        return StepViewHolder(view)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        val instruction = steps[position]
        holder.stepNumber.text = (position + 1).toString()
        holder.stepText.text = instruction

        // Установка иконки по ключевым словам
        val iconRes = when {
            instruction.contains("walk", true) || instruction.contains("пешком", true) -> R.drawable.ic_walk
            instruction.contains("bus", true) || instruction.contains("transit", true) || instruction.contains("автобус", true) -> R.drawable.ic_bus
            instruction.contains("drive", true) || instruction.contains("поезжайте", true) -> R.drawable.ic_drive
            instruction.contains("turn", true) || instruction.contains("поверните", true) -> R.drawable.ic_turn
            else -> R.drawable.ic_direction // по умолчанию
        }
        holder.stepIcon.setImageResource(iconRes)
    }

    override fun getItemCount(): Int = steps.size
}
