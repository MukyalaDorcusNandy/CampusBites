package com.example.campusbite.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.google.firebase.storage.FirebaseStorage
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.campusbite.models.FoodItem
import com.example.campusbite.services.FirebaseService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    FirebaseService()

    var selectedTab by remember { mutableIntStateOf(0) } // 0=Orders, 1=Manage Foods, 2=Add Food
    var orders by remember { mutableStateOf<List<OrderData>>(emptyList()) }
    var menuItems by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Add Food form fields
    var foodName by remember { mutableStateOf(TextFieldValue("")) }
    var foodPrice by remember { mutableStateOf(TextFieldValue("")) }
    var foodDescription by remember { mutableStateOf(TextFieldValue("")) }
    var foodCategory by remember { mutableStateOf("Main") }
    var isAvailable by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // ✅ ADDED: Image upload fields
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isImageUploading by remember { mutableStateOf(false) }

    // ✅ ADDED: Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    val categories = listOf("Breakfast", "Main", "Snacks", "Drinks")

    // Load orders (UNCHANGED)
    fun loadOrders() {
        scope.launch {
            try {
                val snapshot = db.collection("orders")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()

                orders = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data
                    if (data != null) {
                        val itemsList = data["items"] as? List<*> ?: emptyList<Any>()
                        val itemsText = itemsList.joinToString(", ") { item ->
                            val itemMap = item as? Map<*, *>
                            val name = itemMap?.get("foodItemName") as? String ?: ""
                            val qty = (itemMap?.get("quantity") as? Long)?.toInt() ?: 0
                            "$name x$qty"
                        }

                        OrderData(
                            orderId = doc.id,
                            items = itemsText,
                            totalAmount = (data["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                            status = data["status"] as? String ?: "PENDING",
                            userId = data["userId"] as? String ?: ""
                        )
                    } else null
                }
                isLoading = false
            } catch (e: Exception) {
                println("Error loading orders: ${e.message}")
                isLoading = false
            }
        }
    }

    // FIXED: Load menu items - ADDED imageUrl field
    fun loadMenuItems() {
        scope.launch {
            try {
                val snapshot = db.collection("menu_items").get().await()
                menuItems = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data
                    if (data != null) {
                        FoodItem(
                            id = doc.id,
                            name = data["name"] as? String ?: "",
                            price = (data["price"] as? Number)?.toDouble() ?: 0.0,
                            description = data["description"] as? String ?: "",
                            category = data["category"] as? String ?: "",
                            isAvailable = data["isAvailable"] as? Boolean ?: true,
                            imageUrl = data["imageUrl"] as? String ?: ""  // ✅ ADDED THIS LINE
                        )
                    } else null
                }
            } catch (e: Exception) {
                println("Error loading menu items: ${e.message}")
            }
        }
    }

    // Update order status (UNCHANGED)
    fun updateOrderStatus(orderId: String, newStatus: String) {
        scope.launch {
            try {
                db.collection("orders").document(orderId)
                    .update("status", newStatus)
                    .await()
                loadOrders()
            } catch (e: Exception) {
                println("Error updating order: ${e.message}")
            }
        }
    }

    // ✅ ADDED: Upload image to Firebase Storage
    suspend fun uploadImage(uri: Uri): String? {
        return try {
            val fileName = "food_images/${UUID.randomUUID()}.jpg"
            val storageRef = FirebaseStorage.getInstance().reference.child(fileName)
            storageRef.putFile(uri).await()
            val downloadUrl = storageRef.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            println("Error uploading image: ${e.message}")
            null
        }
    }

    // Add new food item (MODIFIED TO ADD IMAGE URL)
    fun addFoodItem() {
        scope.launch {
            isUploading = true
            try {
                val price = foodPrice.text.replace(",", "").toDoubleOrNull() ?: 0.0

                // Upload image if selected
                var uploadedImageUrl = ""
                if (selectedImageUri != null) {
                    isImageUploading = true
                    uploadedImageUrl = uploadImage(selectedImageUri!!) ?: ""
                    isImageUploading = false
                }

                val foodData = hashMapOf(
                    "name" to foodName.text,
                    "price" to price,
                    "description" to foodDescription.text,
                    "category" to foodCategory,
                    "isAvailable" to isAvailable,
                    "imageUrl" to uploadedImageUrl,  // ✅ SAVE IMAGE URL
                    "createdAt" to System.currentTimeMillis()
                )

                db.collection("menu_items").add(foodData).await()

                // Reset form
                foodName = TextFieldValue("")
                foodPrice = TextFieldValue("")
                foodDescription = TextFieldValue("")
                foodCategory = "Main"
                isAvailable = true
                selectedImageUri = null
                showSuccessDialog = true

                loadMenuItems()
            } catch (e: Exception) {
                println("Error adding food: ${e.message}")
            }
            isUploading = false
        }
    }

    // Delete food item (UNCHANGED)
    fun deleteFoodItem(foodId: String) {
        scope.launch {
            try {
                db.collection("menu_items").document(foodId).delete().await()
                loadMenuItems()
            } catch (e: Exception) {
                println("Error deleting food: ${e.message}")
            }
        }
    }

    // Initial load (UNCHANGED)
    LaunchedEffect(Unit) {
        loadOrders()
        loadMenuItems()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF6B00),
                    titleContentColor = Color.White
                ),
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Logout", color = Color.White)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFFFF6B00),
                contentColor = Color.White
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Orders", color = Color.White) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Manage Foods", color = Color.White) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Add Food", color = Color.White) }
                )
            }

            when (selectedTab) {
                0 -> OrdersTab(
                    orders = orders,
                    isLoading = isLoading,
                    onUpdateStatus = { orderId, status -> updateOrderStatus(orderId, status) }
                )
                1 -> ManageFoodsTab(
                    menuItems = menuItems,
                    onDeleteFood = { foodId -> deleteFoodItem(foodId) },
                    onToggleAvailability = { foodId, isAvailable ->
                        scope.launch {
                            db.collection("menu_items").document(foodId)
                                .update("isAvailable", !isAvailable)
                                .await()
                            loadMenuItems()
                        }
                    }
                )
                2 -> AddFoodTab(
                    foodName = foodName,
                    onFoodNameChange = { foodName = it },
                    foodPrice = foodPrice,
                    onFoodPriceChange = { foodPrice = it },
                    foodDescription = foodDescription,
                    onFoodDescriptionChange = { foodDescription = it },
                    foodCategory = foodCategory,
                    onFoodCategoryChange = { foodCategory = it },
                    isAvailable = isAvailable,
                    onIsAvailableChange = { isAvailable = it },
                    categories = categories,
                    isUploading = isUploading,
                    isImageUploading = isImageUploading,
                    selectedImageUri = selectedImageUri,
                    onSelectImage = { imagePickerLauncher.launch("image/*") },
                    onAddFood = { addFoodItem() }
                )
            }
        }
    }

    // Success Dialog (UNCHANGED)
    if (showSuccessDialog) {
        Dialog(onDismissRequest = { showSuccessDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("✅", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Food Item Added!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showSuccessDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))
                    ) {
                        Text("Continue")
                    }
                }
            }
        }
    }
}

// Orders Tab (UNCHANGED)
@Composable
fun OrdersTab(
    orders: List<OrderData>,
    isLoading: Boolean,
    onUpdateStatus: (String, String) -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFFF6B00))
        }
    } else {
        val pendingOrders = orders.filter { it.status == "PENDING" }
        val preparingOrders = orders.filter { it.status == "PREPARING" }
        val readyOrders = orders.filter { it.status == "READY_FOR_PICKUP" }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard("Pending", pendingOrders.size, Color(0xFFFF9800), Modifier.weight(1f))
                    StatCard("In Progress", preparingOrders.size, Color(0xFF2196F3), Modifier.weight(1f))
                    StatCard("Ready", readyOrders.size, Color(0xFF4CAF50), Modifier.weight(1f))
                }
            }

            if (readyOrders.isNotEmpty()) {
                item {
                    Text(
                        text = "READY FOR PICKUP (${readyOrders.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D1B0E)
                    )
                }
                items(readyOrders) { order ->
                    OrderCard(order = order, onUpdateStatus = onUpdateStatus)
                }
            }

            val otherOrders = pendingOrders + preparingOrders
            if (otherOrders.isNotEmpty()) {
                item {
                    Text(
                        text = "ACTIVE ORDERS (${otherOrders.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D1B0E),
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                items(otherOrders) { order ->
                    OrderCard(order = order, onUpdateStatus = onUpdateStatus)
                }
            }

            if (orders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No orders yet", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(90.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = count.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = title, fontSize = 12.sp, color = Color.White)
        }
    }
}

@Composable
fun OrderCard(order: OrderData, onUpdateStatus: (String, String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Order #${order.orderId.take(8)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (order.status) {
                        "PENDING" -> Color(0xFFFF9800)
                        "PREPARING" -> Color(0xFF2196F3)
                        "READY_FOR_PICKUP" -> Color(0xFF4CAF50)
                        else -> Color.Gray
                    }
                ) {
                    Text(
                        text = order.status.replace("_", " "),
                        fontSize = 10.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = order.items, fontSize = 12.sp, color = Color.Gray, maxLines = 2)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Total: UGX ${NumberFormat.getNumberInstance(Locale.US).format(order.totalAmount.toInt())}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFFFF6B00)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (order.status == "PENDING") {
                    Button(
                        onClick = { onUpdateStatus(order.orderId, "PREPARING") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) { Text("Start Preparing", fontSize = 12.sp) }
                }
                if (order.status == "PREPARING") {
                    Button(
                        onClick = { onUpdateStatus(order.orderId, "READY_FOR_PICKUP") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) { Text("Mark Ready", fontSize = 12.sp) }
                }
                if (order.status == "READY_FOR_PICKUP") {
                    Button(
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) { Text("Waiting for Customer", fontSize = 11.sp) }
                }
            }
        }
    }
}

// Manage Foods Tab (UNCHANGED)
@Composable
fun ManageFoodsTab(
    menuItems: List<FoodItem>,
    onDeleteFood: (String) -> Unit,
    onToggleAvailability: (String, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (menuItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) { Text("No food items found. Add some!", color = Color.Gray) }
            }
        } else {
            items(menuItems) { item ->
                FoodManageCard(
                    foodItem = item,
                    onDelete = { onDeleteFood(item.id) },
                    onToggleAvailability = { onToggleAvailability(item.id, item.isAvailable) }
                )
            }
        }
    }
}

@Composable
fun FoodManageCard(
    foodItem: FoodItem,
    onDelete: () -> Unit,
    onToggleAvailability: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = foodItem.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D1B0E))
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (foodItem.isAvailable) Color(0xFF4CAF50) else Color(0xFFF44336)
                    ) {
                        Text(
                            text = if (foodItem.isAvailable) "Available" else "Unavailable",
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "UGX ${NumberFormat.getNumberInstance(Locale.US).format(foodItem.price.toInt())}",
                    fontSize = 14.sp,
                    color = Color(0xFFFF6B00),
                    fontWeight = FontWeight.Medium
                )
                Text(text = foodItem.description.take(50), fontSize = 11.sp, color = Color.Gray)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onToggleAvailability,
                    modifier = Modifier.width(100.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (foodItem.isAvailable) Color(0xFFF44336) else Color(0xFF4CAF50))
                ) { Text(if (foodItem.isAvailable) "Disable" else "Enable", fontSize = 11.sp) }
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.width(100.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336))
                ) { Text("Delete", fontSize = 11.sp, color = Color(0xFFF44336)) }
            }
        }
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Food Item") },
            text = { Text("Are you sure you want to delete ${foodItem.name}?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("Delete", color = Color(0xFFF44336)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}

// Add Food Tab (UNCHANGED - Already has image upload)
@Composable
fun AddFoodTab(
    foodName: TextFieldValue,
    onFoodNameChange: (TextFieldValue) -> Unit,
    foodPrice: TextFieldValue,
    onFoodPriceChange: (TextFieldValue) -> Unit,
    foodDescription: TextFieldValue,
    onFoodDescriptionChange: (TextFieldValue) -> Unit,
    foodCategory: String,
    onFoodCategoryChange: (String) -> Unit,
    isAvailable: Boolean,
    onIsAvailableChange: (Boolean) -> Unit,
    categories: List<String>,
    isUploading: Boolean,
    isImageUploading: Boolean,
    selectedImageUri: Uri?,
    onSelectImage: () -> Unit,
    onAddFood: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectImage() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Color(0xFFFFE3C2)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isImageUploading -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(0xFFFF6B00))
                                Text("Uploading...", fontSize = 12.sp)
                            }
                        }
                        selectedImageUri != null -> {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Selected food image",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📷", fontSize = 48.sp)
                                Text("Tap to upload food photo", fontSize = 14.sp, color = Color(0xFFFF6B00))
                                Text("PNG, JPG up to 5MB", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = foodName,
                onValueChange = onFoodNameChange,
                label = { Text("Food Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("e.g., Chapati, Rolex, Pilau") }
            )
        }

        item {
            OutlinedTextField(
                value = foodPrice,
                onValueChange = onFoodPriceChange,
                label = { Text("Price (UGX)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("e.g., 5000") },
                leadingIcon = { Text("UGX ", fontSize = 14.sp) }
            )
        }

        item {
            OutlinedTextField(
                value = foodDescription,
                onValueChange = onFoodDescriptionChange,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                maxLines = 3,
                placeholder = { Text("Describe the food item...") }
            )
        }

        item {
            Text(text = "Category", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    FilterChip(
                        selected = foodCategory == category,
                        onClick = { onFoodCategoryChange(category) },
                        label = { Text(category, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Currently Available", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = isAvailable,
                    onCheckedChange = onIsAvailableChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF6B00))
                )
            }
        }

        item {
            Button(
                onClick = onAddFood,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00)),
                enabled = !isUploading && foodName.text.isNotBlank() && foodPrice.text.isNotBlank()
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publishing...")
                } else {
                    Text("Publish Food", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Data class for orders (UNCHANGED)
data class OrderData(
    val orderId: String,
    val items: String,
    val totalAmount: Double,
    val status: String,
    val userId: String
)