package com.example.campusbite.screens


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.campusbite.models.FoodItem
import com.example.campusbite.viewmodels.AuthViewModel
import com.example.campusbite.viewmodels.AuthState
import com.example.campusbite.viewmodels.CartViewModel
import com.example.campusbite.viewmodels.MenuViewModel
import java.text.NumberFormat
import java.util.Locale

private fun formatUgx(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "UGX ${formatter.format(amount.toInt())}"
}

sealed class BottomNavScreen(val route: String, val title: String, val icon: String) {
    object Menu : BottomNavScreen("menu", "Menu", "🍔")
    object Cart : BottomNavScreen("cart", "Cart", "🛒")
    object Profile : BottomNavScreen("profile", "Profile", "👤")
    object Admin : BottomNavScreen("admin", "Admin", "👨‍🍳")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    menuViewModel: MenuViewModel,
    cartViewModel: CartViewModel,
    authViewModel: AuthViewModel,
    onFoodItemClick: (FoodItem) -> Unit,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()
    val currentUser = (authState as? AuthState.Authenticated)?.user
    val isAdmin = currentUser?.email == "admin@campusbite.com"
    val cartItems by cartViewModel.cartItems.collectAsState()
    val cartItemCount = cartItems.sumOf { it.quantity }

    // ADDED: Check for active order
    val activeOrderId by authViewModel.activeOrderId.collectAsState()
    val hasActiveOrder = activeOrderId != null

    val screens = buildList {
        add(BottomNavScreen.Menu)
        add(BottomNavScreen.Cart)
        add(BottomNavScreen.Profile)
        if (isAdmin) {
            add(BottomNavScreen.Admin)
        }
    }

    Scaffold(
        topBar = {
            // ADDED: Banner for active order
            if (hasActiveOrder) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate("orderStatus")
                        },
                    color = Color(0xFFFF6B00)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🕒", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "You have an active order! Tap here to check status",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 0.dp,
                modifier = Modifier.height(65.dp)
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Box {
                                Text(screen.icon, fontSize = 24.sp)
                                if (screen == BottomNavScreen.Cart && cartItemCount > 0) {
                                    Badge(
                                        containerColor = Color(0xFFFF6B00),
                                        modifier = Modifier.offset(x = 12.dp, y = (-8).dp)
                                    ) {
                                        Text(
                                            text = if (cartItemCount > 99) "99+" else "$cartItemCount",
                                            fontSize = 10.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 11.sp,
                                fontWeight = if (currentRoute == screen.route) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFF6B00),
                            selectedTextColor = Color(0xFFFF6B00),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = BottomNavScreen.Menu.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(BottomNavScreen.Menu.route) {
                MenuScreen(
                    menuViewModel = menuViewModel,
                    cartViewModel = cartViewModel,
                    onNavigateToCart = {
                        navController.navigate(BottomNavScreen.Cart.route)
                    },
                    onNavigateToProfile = {
                        navController.navigate(BottomNavScreen.Profile.route)
                    },
                    onFoodItemClick = onFoodItemClick
                )
            }

            composable(BottomNavScreen.Cart.route) {
                CartScreen(
                    cartViewModel = cartViewModel,
                    onConfirmOrder = {
                        navController.navigate("orderStatus")
                    },
                    onBack = {
                        navController.popBackStack()
                    },
                    userId = currentUser?.uid ?: ""
                )
            }

            composable(BottomNavScreen.Profile.route) {
                ProfileScreen(
                    authViewModel = authViewModel,
                    onLogout = onLogout,
                    navController = navController
                )
            }

            if (isAdmin) {
                composable(BottomNavScreen.Admin.route) {
                    AdminScreen(
                        onLogout = onLogout
                    )
                }
            }

            composable("orderStatus") {
                OrderStatusScreen(
                    cartViewModel = cartViewModel,
                    onBackToMenu = {
                        authViewModel.clearActiveOrder()
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}