package com.example.campusbite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.campusbite.screens.*
import com.example.campusbite.ui.theme.CampusBiteTheme
import com.example.campusbite.viewmodels.AuthViewModel
import com.example.campusbite.viewmodels.AuthState
import com.example.campusbite.viewmodels.CartViewModel
import com.example.campusbite.viewmodels.MenuViewModel
import com.example.campusbite.models.FoodItem

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CampusBiteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CampusBiteApp()
                }
            }
        }
    }
}

@Composable
fun CampusBiteApp() {
    val authViewModel: AuthViewModel = viewModel()
    val menuViewModel: MenuViewModel = viewModel()
    val cartViewModel: CartViewModel = viewModel()

    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()
    val activeOrderId by authViewModel.activeOrderId.collectAsState()
    val currentUser = (authState as? AuthState.Authenticated)?.user

    var selectedFoodItem by remember { mutableStateOf<FoodItem?>(null) }

    // Check if user is admin
    val isAdmin = currentUser?.email?.equals("admin@campusbite.com", ignoreCase = true) == true

    // Get the active order ID value safely
    val activeOrderIdValue = activeOrderId

    // Determine start destination
    val startDestination = when {
        authState !is AuthState.Authenticated -> "login"
        isAdmin -> "admin"
        else -> "main"  // Always go to main screen with bottom navigation
    }

    // Pass active order ID to CartViewModel if exists
    LaunchedEffect(activeOrderIdValue) {
        if (activeOrderIdValue != null) {
            // Set the current order ID in cartViewModel
            cartViewModel.setCurrentOrderId(activeOrderIdValue)
            // Listen to order status updates
            cartViewModel.listenToOrderStatus(activeOrderIdValue) { status ->
                cartViewModel.updateOrderStatus(status)
            }
        }
    }

    // ✅ ADDED: Restore active order to CartViewModel after login (for second login)
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated && !isAdmin) {
            val activeId = authViewModel.getActiveOrderId()
            if (activeId != null && activeId.isNotEmpty()) {
                cartViewModel.setCurrentOrderId(activeId)
                println("✅ Restored active order to CartViewModel: $activeId")
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Login Screen
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = { isAdminUser ->
                    if (isAdminUser) {
                        navController.navigate("admin") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        // Always go to main even if there's an active order
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }

        // Main Screen for Students (WITH BOTTOM NAVIGATION)
        composable("main") {
            MainScreen(
                menuViewModel = menuViewModel,
                cartViewModel = cartViewModel,
                authViewModel = authViewModel,
                onFoodItemClick = { foodItem ->
                    selectedFoodItem = foodItem
                    navController.navigate("foodDetails")
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }

        // Food Details Screen
        composable("foodDetails") {
            selectedFoodItem?.let { foodItem ->
                FoodDetailsScreen(
                    foodItem = foodItem,
                    cartViewModel = cartViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Cart Screen
        composable("cart") {
            CartScreen(
                cartViewModel = cartViewModel,
                onConfirmOrder = {
                    navController.navigate("orderStatus")
                },
                onBack = { navController.popBackStack() },
                userId = currentUser?.uid ?: ""
            )
        }

        // Order Status Screen (accessible from Cart after placing order)
        composable("orderStatus") {
            OrderStatusScreen(
                cartViewModel = cartViewModel,
                onBackToMenu = {
                    authViewModel.clearActiveOrder()
                    navController.popBackStack("main", inclusive = false)
                }
            )
        }

        // Admin Screen - Kitchen Dashboard
        composable("admin") {
            AdminScreen(
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("admin") { inclusive = true }
                    }
                }
            )
        }
    }
}