package com.example.campusbite.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.campusbite.models.AppOrder
import com.example.campusbite.models.OrderItem
import com.example.campusbite.services.FirebaseService
import com.example.campusbite.viewmodels.AuthViewModel
import com.example.campusbite.viewmodels.AuthState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.Locale

private fun formatUgx(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "UGX ${formatter.format(amount.toInt())}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    navController: NavController
) {
    val authState by authViewModel.authState.collectAsState()
    val currentUser = (authState as? AuthState.Authenticated)?.user

    val db = FirebaseFirestore.getInstance()
    val firebaseService = FirebaseService()

    var orderHistory by remember { mutableStateOf<List<AppOrder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var userName by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }

    // DEBUG: Show on screen
    var debugText by remember { mutableStateOf("Loading...") }
    var orderCountText by remember { mutableStateOf("") }

    val isAdmin = currentUser?.email == "admin@campusbite.com"

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            isLoading = true

            debugText = "📱 Checking: ${currentUser.email}"

            try {
                val userDoc = db.collection("users").document(currentUser.uid).get().await()
                if (userDoc.exists()) {
                    userName = userDoc.getString("name") ?: currentUser.email?.split("@")?.first() ?: "Student"
                    studentId = userDoc.getString("studentId") ?: "STU${currentUser.uid.take(8).uppercase()}"
                } else {
                    userName = currentUser.email?.split("@")?.first() ?: "Student"
                    studentId = "STU${currentUser.uid.take(8).uppercase()}"
                }

                debugText = "🆔 UID: ${currentUser.uid}\n📧 ${currentUser.email}"

                val result = firebaseService.getUserOrders(currentUser.uid)
                result.onSuccess { orders ->
                    orderHistory = orders
                    orderCountText = "✅ Found ${orders.size} orders"
                    debugText = "$debugText\n$orderCountText"
                }.onFailure { error ->
                    orderCountText = "❌ Error: ${error.message}"
                    debugText = "$debugText\n$orderCountText"
                }
            } catch (e: Exception) {
                debugText = "❌ Exception: ${e.message}"
            }

            isLoading = false
        } else {
            isLoading = false
            debugText = "⚠️ No user logged in"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF6B00),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFE3C2))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = {
                                            navController.navigate("admin")
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👨‍🎓", fontSize = 40.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = userName.ifEmpty { currentUser?.email?.split("@")?.firstOrNull() ?: "Student" },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D1B0E)
                        )

                        Text(
                            text = currentUser?.email ?: "student@university.edu",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFE3C2),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = studentId,
                                fontSize = 12.sp,
                                color = Color(0xFFFF6B00),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                        ) {
                            Text(
                                text = debugText,
                                fontSize = 11.sp,
                                color = Color(0xFF333333),
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        if (isAdmin) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { navController.navigate("admin") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))
                            ) {
                                Text("👨‍🍳 Kitchen Dashboard", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Order History",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D1B0E)
                    )
                    Text(
                        text = "${orderHistory.size} orders",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFF6B00))
                    }
                }
            } else if (orderHistory.isNotEmpty()) {
                items(orderHistory) { order ->
                    OrderHistoryCard(order = order)
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🛒", fontSize = 56.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No orders yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2D1B0E)
                            )
                            Text(
                                text = "Place an order to see it here",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336),
                        contentColor = Color.White
                    )
                ) {
                    Text("Logout", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun OrderHistoryCard(order: AppOrder) {
    // Create a summary of items for the title (e.g., "2x Chapati, 1x Rolex")
    val orderSummary = remember(order.items) {
        if (order.items.isEmpty()) {
            "Order"
        } else {
            order.items.joinToString(", ") { item ->
                "${item.quantity}x ${item.foodItemName}"
            }
        }
    }

    // Get icon based on first item
    val firstItem = order.items.firstOrNull()
    val foodIcon = when {
        firstItem?.foodItemName?.contains("Juice", ignoreCase = true) == true -> "🥤"
        firstItem?.foodItemName?.contains("Chapati", ignoreCase = true) == true -> "🫓"
        firstItem?.foodItemName?.contains("Rice", ignoreCase = true) == true -> "🍚"
        firstItem?.foodItemName?.contains("Chicken", ignoreCase = true) == true -> "🍗"
        firstItem?.foodItemName?.contains("Pilau", ignoreCase = true) == true -> "🍛"
        firstItem?.foodItemName?.contains("Rolex", ignoreCase = true) == true -> "🌯"
        firstItem?.foodItemName?.contains("Mandazi", ignoreCase = true) == true -> "🍩"
        firstItem?.foodItemName?.contains("Soda", ignoreCase = true) == true -> "🥤"
        firstItem?.foodItemName?.contains("Chips", ignoreCase = true) == true -> "🍟"
        else -> "🍽️"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Food Icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFE3C2)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = foodIcon, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Order details - SHOW FOOD ITEMS instead of Order ID
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = orderSummary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF2D1B0E),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = android.text.format.DateFormat.format("MMM dd, yyyy • hh:mm a", order.timestamp).toString(),
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                if (order.estimatedPickupTime.isNotEmpty()) {
                    Text(
                        text = "Pickup: ${order.estimatedPickupTime}",
                        fontSize = 9.sp,
                        color = Color(0xFFFF6B00)
                    )
                }
            }

            // Price and Status
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatUgx(order.totalAmount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFFFF6B00)
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (order.status) {
                        "COMPLETED" -> Color(0xFF4CAF50)
                        "CANCELLED" -> Color(0xFFF44336)
                        "READY_FOR_PICKUP" -> Color(0xFFFF9800)
                        "PREPARING" -> Color(0xFF2196F3)
                        else -> Color(0xFF9E9E9E)
                    },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = when (order.status) {
                            "COMPLETED" -> "Completed"
                            "CANCELLED" -> "Cancelled"
                            "READY_FOR_PICKUP" -> "Ready"
                            "PREPARING" -> "Preparing"
                            "CONFIRMED" -> "Confirmed"
                            "PENDING" -> "Pending"
                            else -> order.status.replace("_", " ")
                        },
                        fontSize = 9.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}