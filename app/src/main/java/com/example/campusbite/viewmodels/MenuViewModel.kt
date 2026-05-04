package com.example.campusbite.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbite.models.FoodItem
import com.example.campusbite.services.FirebaseService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MenuViewModel : ViewModel() {
    private val firebaseService = FirebaseService()

    private val _menuItems = MutableStateFlow<List<FoodItem>>(emptyList())
    val menuItems: StateFlow<List<FoodItem>> = _menuItems.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private var _allCategories = mutableSetOf<String>()

    // Dynamic categories from Firebase
    fun getCategories(): List<String> {
        val categories = _allCategories.toList().sorted()
        return listOf("All") + categories
    }

    init {
        loadMenuItems()
    }

    fun loadMenuItems() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = ""

            val result = firebaseService.getMenuItems()
            result.onSuccess { items ->
                _menuItems.value = items
                // Collect unique categories from actual data
                _allCategories.clear()
                items.forEach { item ->
                    if (item.category.isNotEmpty()) {
                        _allCategories.add(item.category)
                    }
                }
                println("✅ Loaded ${items.size} items")
                println("📁 Categories found: ${_allCategories}")
            }.onFailure { exception ->
                _errorMessage.value = exception.message ?: "Failed to load menu"
                println("❌ Error: ${exception.message}")
            }

            _isLoading.value = false
        }
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun getFilteredItems(): List<FoodItem> {
        return if (_selectedCategory.value == "All") {
            _menuItems.value
        } else {
            _menuItems.value.filter {
                it.category.equals(_selectedCategory.value, ignoreCase = true)
            }
        }
    }

    fun seedMenuItems(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                firebaseService.seedMenuItems()
                onComplete(true)
            } catch (e: Exception) {
                println("Error seeding menu: ${e.message}")
                onComplete(false)
            }
        }
    }
}