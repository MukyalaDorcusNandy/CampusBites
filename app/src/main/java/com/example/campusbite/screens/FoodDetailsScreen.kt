package com.example.campusbite.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusbite.models.FoodItem
import com.example.campusbite.ui.theme.CampusBiteTheme
import com.example.campusbite.viewmodels.CartViewModel
import java.text.NumberFormat
import java.util.Locale

private fun formatUgx(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "UGX ${formatter.format(amount.toInt())}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailsScreen(
    foodItem: FoodItem,
    cartViewModel: CartViewModel,
    onBack: () -> Unit
) {
    var quantity by remember { mutableStateOf(1) }
    val totalPrice = foodItem.price * quantity

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = foodItem.name,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 26.sp)
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    repeat(quantity) {
                        cartViewModel.addToCart(foodItem)
                    }
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))
            ) {
                Text(
                    text = "Add to Cart • ${formatUgx(totalPrice)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFFBF4))
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // REAL FOOD IMAGE from drawable folder
            Image(
                painter = painterResource(id = foodItem.getImageRes()),
                contentDescription = foodItem.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFFFE3C2)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = foodItem.name,
                fontSize = 27.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF2D1B0E)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = formatUgx(foodItem.price),
                fontSize = 21.sp,
                color = Color(0xFFFF6B00),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Description",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D1B0E)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = foodItem.description,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = Color(0xFF6F5B4B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Quantity Selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Quantity",
                            fontSize = 14.sp,
                            color = Color(0xFF78685C)
                        )
                        Text(
                            text = "Select number of plates/items",
                            fontSize = 12.sp,
                            color = Color(0xFF9B8B7F)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OutlinedIconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            enabled = quantity > 1,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Text("−", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFE3C2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$quantity",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D1B0E)
                            )
                        }

                        OutlinedIconButton(
                            onClick = { quantity++ },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// FIXED PREVIEW - No ViewModel construction warning
@Preview(showBackground = true)
@Composable
fun PreviewFoodDetailsScreen() {
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
                Text("🍽️", fontSize = 60.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Food Details", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Tap on a food item to see details", fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}