package com.example.campusbite.models

import java.util.UUID

data class Order(
    val orderId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val items: List<CartItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val status: OrderStatus = OrderStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis(),
    val estimatedPickupTime: String = ""
)

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
    READY_FOR_PICKUP,
    COMPLETED,
    CANCELLED
}