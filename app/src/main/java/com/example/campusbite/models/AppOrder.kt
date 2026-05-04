package com.example.campusbite.models

data class AppOrder(
    val orderId: String,
    val userId: String,
    val totalAmount: Double,
    val status: String,
    val timestamp: Long,
    val estimatedPickupTime: String = "",
    val items: List<OrderItem> = emptyList()  // ← Add this field
)

data class OrderItem(
    val foodItemId: String,
    val foodItemName: String,
    val price: Double,
    val quantity: Int,
    val totalPrice: Double
)