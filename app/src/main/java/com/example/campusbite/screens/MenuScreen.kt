package com.example.campusbite.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.campusbite.models.FoodItem
import com.example.campusbite.ui.theme.CampusBiteTheme
import com.example.campusbite.viewmodels.CartViewModel
import com.example.campusbite.viewmodels.MenuViewModel
import java.text.NumberFormat
import java.util.Locale

private fun formatUgx(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "UGX ${formatter.format(amount.toInt())}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    menuViewModel: MenuViewModel,
    cartViewModel: CartViewModel,
    onNavigateToCart: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onFoodItemClick: (FoodItem) -> Unit
) {
    val categories = menuViewModel.getCategories()
    val selectedCategory by menuViewModel.selectedCategory.collectAsState()
    val filteredItems = menuViewModel.getFilteredItems()
    val isLoading by menuViewModel.isLoading.collectAsState()
    val cartItemCount = cartViewModel.getItemCount()

    // Debug print
    LaunchedEffect(selectedCategory, filteredItems) {
        println("========== MENU DEBUG ==========")
        println("Selected category: '$selectedCategory'")
        println("Filtered items count: ${filteredItems.size}")
        filteredItems.forEach { item ->
            println("  - ${item.name} (${item.category})")
        }
        println("================================")
    }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("CampusBite", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Fresh campus meals",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Text("👤", fontSize = 20.sp)
                    }

                    BadgedBox(
                        badge = {
                            if (cartItemCount > 0) {
                                Badge { Text("$cartItemCount") }
                            }
                        }
                    ) {
                        IconButton(onClick = onNavigateToCart) {
                            Text("🛒", fontSize = 20.sp)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFFFBF4))
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "What would you like to eat today?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color(0xFF2D1B0E)
                    )
                    Text(
                        text = "Choose a category and order your favourite campus food.",
                        fontSize = 13.sp,
                        color = Color(0xFF6F5B4B),
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            FilterChip(
                                onClick = { menuViewModel.setCategory(category) },
                                label = { Text(category) },
                                selected = selectedCategory == category,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFF6B00),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                items(filteredItems, key = { it.id }) { item ->
                    FoodMenuCard(
                        item = item,
                        onCardClick = { onFoodItemClick(item) },
                        onAddClick = {
                            cartViewModel.addToCart(item)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FoodMenuCard(
    item: FoodItem,
    onCardClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ONLY THIS PART CHANGED - Image display with uploaded image support
            // Shows uploaded image if available, otherwise shows local drawable
            if (item.imageUrl.isNotBlank()) {
                // Show uploaded image from gallery
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFE3C2)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Show local drawable (original code - unchanged)
                Image(
                    painter = painterResource(id = item.getImageRes()),
                    contentDescription = item.name,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFE3C2)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF2D1B0E)
                )

                Text(
                    text = item.description.take(50),
                    fontSize = 12.sp,
                    color = Color(0xFF78685C),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = formatUgx(item.price),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B00),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Button(
                onClick = onAddClick,
                modifier = Modifier
                    .height(36.dp)
                    .width(72.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))
            ) {
                Text("Add", fontSize = 12.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMenuScreen() {
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
                Text("🍔", fontSize = 60.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("CampusBite Menu", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6B00))
                Text("Run the app to see menu items", fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}