package com.example.campusbite.models

import com.example.campusbite.R

data class FoodItem(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val description: String = "",
    val category: String = "",
    val isAvailable: Boolean = true,
    val imageUrl: String = ""

) {
    // Map food name to actual image resource from drawable folder
    fun getImageRes(): Int {
        return when (name.lowercase()) {
            "chapati" -> R.drawable.food_chapati
            "chips & chicken", "chips and chicken" -> R.drawable.food_chips_chicken
            "fresh juice", "juice" -> R.drawable.food_juice
            "mandazi" -> R.drawable.food_mandazi
            "chicken pilau", "pilau" -> R.drawable.food_pilau
            "rice & beans", "rice and beans" -> R.drawable.food_rice_beans
            "rolex" -> R.drawable.food_rolex
            "soda" -> R.drawable.food_soda
            else -> R.drawable.food_placeholder
        }
    }
}