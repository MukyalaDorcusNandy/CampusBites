package com.example.campusbite.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbite.services.FirebaseService
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val firebaseService = FirebaseService()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    private val _activeOrderId = MutableStateFlow<String?>(null)
    val activeOrderId: StateFlow<String?> = _activeOrderId

    init {
        // Do NOT auto-login - user must explicitly enter credentials
    }

    private fun checkForActiveOrder(userId: String) {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("orders")
                    .whereEqualTo("userId", userId)
                    .whereIn("status", listOf("PENDING", "CONFIRMED", "PREPARING"))
                    .get()
                    .await()

                if (snapshot.documents.isNotEmpty()) {
                    val activeOrder = snapshot.documents.first()
                    val orderId = activeOrder.id
                    _activeOrderId.value = orderId
                    println("✅ Found active order: $orderId, status: ${activeOrder.getString("status")}")
                } else {
                    _activeOrderId.value = null
                    println("✅ No active orders found for user: $userId")
                }
            } catch (e: Exception) {
                println("❌ Error checking active order: ${e.message}")
                _activeOrderId.value = null
            }
        }
    }

    // ✅ ADD THIS: Getter for active order ID
    fun getActiveOrderId(): String? = _activeOrderId.value

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = ""

            val result = firebaseService.login(email, password)
            result.onSuccess { user ->
                _authState.value = AuthState.Authenticated(user)
                checkForActiveOrder(user.uid)
            }.onFailure { exception ->
                _errorMessage.value = exception.message ?: "Login failed"
            }

            _isLoading.value = false
        }
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = ""

            val result = firebaseService.register(name, email, password)
            result.onSuccess { user ->
                _errorMessage.value = "✅ Registration successful! Please login."
            }.onFailure { exception ->
                _errorMessage.value = exception.message ?: "Registration failed"
            }

            _isLoading.value = false
        }
    }

    fun logout() {
        firebaseService.logout()
        _authState.value = AuthState.Unauthenticated
        _activeOrderId.value = null
    }

    fun getCurrentUser(): FirebaseUser? = firebaseService.getCurrentUser()

    fun clearActiveOrder() {
        _activeOrderId.value = null
    }
}

sealed class AuthState {
    object Unauthenticated : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    object Loading : AuthState()
}