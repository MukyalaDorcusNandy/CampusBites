package com.example.campusbite.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbite.models.CartItem
import com.example.campusbite.models.FoodItem
import com.example.campusbite.services.FirebaseService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CartViewModel : ViewModel() {
    private val firebaseService = FirebaseService()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _currentOrderId = MutableStateFlow<String?>(null)
    val currentOrderId: StateFlow<String?> = _currentOrderId.asStateFlow()

    private val _orderStatus = MutableStateFlow<String?>(null)
    val orderStatus: StateFlow<String?> = _orderStatus.asStateFlow()

    private val _isPlacingOrder = MutableStateFlow(false)
    val isPlacingOrder: StateFlow<Boolean> = _isPlacingOrder.asStateFlow()

    fun addToCart(foodItem: FoodItem) {
        val currentItems = _cartItems.value.toMutableList()
        val existingItem = currentItems.find { it.foodItem.id == foodItem.id }

        if (existingItem != null) {
            val index = currentItems.indexOf(existingItem)
            currentItems[index] = existingItem.copy(quantity = existingItem.quantity + 1)
        } else {
            currentItems.add(CartItem(foodItem = foodItem, quantity = 1))
        }

        _cartItems.value = currentItems
    }

    fun updateQuantity(cartItem: CartItem, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(cartItem)
            return
        }

        val currentItems = _cartItems.value.toMutableList()
        val index = currentItems.indexOf(cartItem)
        if (index != -1) {
            currentItems[index] = cartItem.copy(quantity = newQuantity)
            _cartItems.value = currentItems
        }
    }

    fun removeFromCart(cartItem: CartItem) {
        _cartItems.value = _cartItems.value.filter { it != cartItem }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun getTotalPrice(): Double {
        return _cartItems.value.sumOf { it.totalPrice }
    }

    fun getItemCount(): Int {
        return _cartItems.value.sumOf { it.quantity }
    }

    fun placeOrder(userId: String, onComplete: (Boolean, String?) -> Unit) {
        if (userId.isEmpty()) {
            onComplete(false, "User not logged in")
            return
        }

        if (_cartItems.value.isEmpty()) {
            onComplete(false, "Cart is empty")
            return
        }

        viewModelScope.launch {
            _isPlacingOrder.value = true

            val result = firebaseService.placeOrder(
                userId = userId,
                items = _cartItems.value,
                totalAmount = getTotalPrice()
            )

            result.onSuccess { orderId ->
                _currentOrderId.value = orderId
                _orderStatus.value = "PENDING"
                clearCart()
                onComplete(true, orderId)
            }.onFailure { exception ->
                onComplete(false, exception.message)
            }

            _isPlacingOrder.value = false
        }
    }

    fun listenToOrderStatus(orderId: String, onStatusUpdate: (String) -> Unit) {
        firebaseService.listenToOrderStatus(orderId, onStatusUpdate)
    }

    fun updateOrderStatus(status: String) {
        _orderStatus.value = status
    }

    fun resetOrder() {
        _currentOrderId.value = null
        _orderStatus.value = null
    }

    // ✅ ADD THIS FUNCTION - Missing function for MainActivity
    fun setCurrentOrderId(orderId: String) {
        _currentOrderId.value = orderId
    }
}