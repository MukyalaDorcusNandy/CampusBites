package com.example.campusbite.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusbite.ui.theme.CampusBiteTheme
import com.example.campusbite.viewmodels.CartViewModel
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderStatusScreen(
    cartViewModel: CartViewModel,
    onBackToMenu: () -> Unit
) {
    val currentOrderId by cartViewModel.currentOrderId.collectAsState()
    var currentStatus by remember { mutableStateOf("PENDING") }
    val db = FirebaseFirestore.getInstance()

    // Listen to real-time order status updates from Firebase
    LaunchedEffect(currentOrderId) {
        if (currentOrderId != null) {
            println("🔄 Listening to order: $currentOrderId")

            // Real-time listener
            db.collection("orders").document(currentOrderId!!)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        println("❌ Listen error: ${error.message}")
                        return@addSnapshotListener
                    }

                    val status = snapshot?.getString("status") ?: "PENDING"
                    if (status != currentStatus) {
                        println("📡 Status updated: $status")
                        currentStatus = status
                        cartViewModel.updateOrderStatus(status)
                    }
                }
        }
    }

    val statusSteps = listOf(
        "PENDING" to "Order Received",
        "CONFIRMED" to "Confirmed",
        "PREPARING" to "Preparing",
        "READY_FOR_PICKUP" to "Ready for Pickup"
    )

    val currentStepIndex = statusSteps.indexOfFirst { it.first == currentStatus }.coerceAtLeast(0)
    val progress = (currentStepIndex + 1) / 4f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Status") },
                navigationIcon = {
                    //  ONLY ADDED THIS - Back button to exit anytime
                    IconButton(onClick = onBackToMenu) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF6B00),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status icon
            Text(
                text = when (currentStatus) {
                    "PENDING" -> "📝"
                    "CONFIRMED" -> "✅"
                    "PREPARING" -> "🍳"
                    "READY_FOR_PICKUP" -> "🛵"
                    else -> "🍕"
                },
                fontSize = 60.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when (currentStatus) {
                    "PENDING" -> "Order Placed!"
                    "CONFIRMED" -> "Order Confirmed!"
                    "PREPARING" -> "Preparing Your Food!"
                    "READY_FOR_PICKUP" -> "Ready for Pickup!"
                    else -> "Processing Your Order"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Order #${currentOrderId?.take(8) ?: "----"}",
                color = Color.Gray,
                fontSize = 12.sp
            )

            Text(
                text = "Kitchen staff will update status in real-time",
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Status steps
            statusSteps.forEachIndexed { index, (status, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    index < currentStepIndex -> Color(0xFF4CAF50)
                                    index == currentStepIndex -> Color(0xFFFF6B00)
                                    else -> Color.LightGray
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            index < currentStepIndex -> Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                            index == currentStepIndex && currentStatus != "READY_FOR_PICKUP" -> {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            }
                            else -> Text("${index + 1}", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (index <= currentStepIndex) FontWeight.Bold else FontWeight.Normal,
                        color = if (index <= currentStepIndex) Color(0xFF2D1B0E) else Color.Gray
                    )
                }

                if (index < statusSteps.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = Color(0xFFFF6B00),
                trackColor = Color.LightGray
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (currentStatus == "READY_FOR_PICKUP") {
                Button(
                    onClick = {
                        cartViewModel.resetOrder()
                        onBackToMenu()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))
                ) {
                    Text("Back to Menu", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewOrderStatusScreen() {
    CampusBiteTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("📦", fontSize = 60.sp)
                Text("Order Status", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Real-time updates from kitchen", fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}