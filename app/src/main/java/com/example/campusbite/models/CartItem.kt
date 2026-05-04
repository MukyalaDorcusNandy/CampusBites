package com.example.campusbite.models

data class CartItem(
    val foodItem: FoodItem,
    val quantity: Int = 1
) {
    val totalPrice: Double
        get() = foodItem.price * quantity

    // For Firestore serialization
    fun toMap(): Map<String, Any> = mapOf(
        "foodItemId" to foodItem.id,
        "foodItemName" to foodItem.name,
        "price" to foodItem.price,
        "quantity" to quantity,
        "totalPrice" to totalPrice
    )
}