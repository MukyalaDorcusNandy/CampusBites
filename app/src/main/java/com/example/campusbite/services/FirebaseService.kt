package com.example.campusbite.services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.example.campusbite.models.FoodItem
import com.example.campusbite.models.AppOrder
import com.example.campusbite.models.OrderItem
import com.example.campusbite.models.CartItem
import kotlinx.coroutines.tasks.await

class FirebaseService {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            println("🔐 Logging in with: $email")
            val result = auth.signInWithEmailAndPassword(email, password).await()
            println("✅ Login successful! UID: ${result.user?.uid}")
            Result.success(result.user!!)
        } catch (e: Exception) {
            println("❌ Login error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun register(name: String, email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user!!

            val userData = hashMapOf(
                "uid" to user.uid,
                "name" to name,
                "email" to email,
                "studentId" to "STU${user.uid.take(8).uppercase()}",
                "createdAt" to System.currentTimeMillis()
            )
            db.collection("users").document(user.uid).set(userData).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun getMenuItems(): Result<List<FoodItem>> {
        return try {
            val snapshot = db.collection("menu_items").get().await()
            val items = snapshot.documents.mapNotNull { doc ->
                val data = doc.data
                if (data != null) {
                    FoodItem(
                        id = doc.id,
                        name = data["name"] as? String ?: "",
                        price = (data["price"] as? Number)?.toDouble() ?: 0.0,
                        description = data["description"] as? String ?: "",
                        category = data["category"] as? String ?: "",
                        isAvailable = data["isAvailable"] as? Boolean ?: true,
                        imageUrl = data["imageUrl"] as? String ?: ""  // ← ADDED THIS LINE
                    )
                } else null
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun seedMenuItems() {
        val menuItems = listOf(
            mapOf("name" to "Chapati", "price" to 2000.0, "description" to "Soft and layered flatbread, perfect with tea or any stew.", "category" to "Breakfast", "isAvailable" to true, "imageUrl" to ""),
            mapOf("name" to "Chips & Chicken", "price" to 12000.0, "description" to "Crispy golden fries served with tender grilled chicken.", "category" to "Main", "isAvailable" to true, "imageUrl" to ""),
            mapOf("name" to "Fresh Juice", "price" to 5000.0, "description" to "Refreshing natural fruit juice. Available in mango, passion, or orange.", "category" to "Drinks", "isAvailable" to true, "imageUrl" to ""),
            mapOf("name" to "Mandazi", "price" to 1500.0, "description" to "East African coconut doughnut, fluffy and lightly sweetened.", "category" to "Snacks", "isAvailable" to true, "imageUrl" to ""),
            mapOf("name" to "Chicken Pilau", "price" to 8000.0, "description" to "Spiced rice cooked with tender chicken and aromatic spices.", "category" to "Main", "isAvailable" to true, "imageUrl" to ""),
            mapOf("name" to "Rice & Beans", "price" to 6000.0, "description" to "Classic combo of steamed rice and savory beans stew.", "category" to "Main", "isAvailable" to true, "imageUrl" to ""),
            mapOf("name" to "Rolex", "price" to 7000.0, "description" to "Ugandan rolled chapati filled with spiced omelette and veggies.", "category" to "Breakfast", "isAvailable" to true, "imageUrl" to ""),
            mapOf("name" to "Soda", "price" to 2500.0, "description" to "Chilled soft drink. Coca-Cola, Fanta, or Sprite.", "category" to "Drinks", "isAvailable" to true, "imageUrl" to "")
        )

        for (item in menuItems) {
            db.collection("menu_items").add(item).await()
        }
    }

    suspend fun placeOrder(userId: String, items: List<CartItem>, totalAmount: Double): Result<String> {
        return try {
            println("📦 Placing order for userId: $userId")
            val order = hashMapOf(
                "userId" to userId,
                "items" to items.map { cartItem ->
                    mapOf(
                        "foodItemId" to cartItem.foodItem.id,
                        "foodItemName" to cartItem.foodItem.name,
                        "price" to cartItem.foodItem.price,
                        "quantity" to cartItem.quantity,
                        "totalPrice" to cartItem.totalPrice
                    )
                },
                "totalAmount" to totalAmount,
                "status" to "PENDING",
                "timestamp" to System.currentTimeMillis(),
                "estimatedPickupTime" to "15-20 minutes"
            )

            val docRef = db.collection("orders").add(order).await()
            println("✅ Order placed with ID: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            println("❌ Error placing order: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getUserOrders(userId: String): Result<List<AppOrder>> {
        return try {
            println("🔍 FirebaseService.getUserOrders() called with userId: $userId")

            val snapshot = db.collection("orders")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            println("🔍 Query returned ${snapshot.documents.size} documents")

            val orders = snapshot.documents.mapNotNull { doc ->
                val data = doc.data
                if (data != null) {
                    // Parse items array
                    val itemsList = mutableListOf<OrderItem>()
                    val itemsArray = data["items"] as? List<*>
                    itemsArray?.forEach { item ->
                        val itemMap = item as? Map<*, *>
                        if (itemMap != null) {
                            itemsList.add(
                                OrderItem(
                                    foodItemId = itemMap["foodItemId"] as? String ?: "",
                                    foodItemName = itemMap["foodItemName"] as? String ?: "",
                                    price = (itemMap["price"] as? Number)?.toDouble() ?: 0.0,
                                    quantity = (itemMap["quantity"] as? Number)?.toInt() ?: 0,
                                    totalPrice = (itemMap["totalPrice"] as? Number)?.toDouble() ?: 0.0
                                )
                            )
                        }
                    }

                    AppOrder(
                        orderId = doc.id,
                        userId = data["userId"] as? String ?: "",
                        totalAmount = (data["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                        status = data["status"] as? String ?: "PENDING",
                        timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0,
                        estimatedPickupTime = data["estimatedPickupTime"] as? String ?: "",
                        items = itemsList
                    )
                } else null
            }

            println("🔍 Mapped to ${orders.size} AppOrder objects")
            orders.forEach { order ->
                println("   Order: ${order.orderId}, Status: ${order.status}, Items: ${order.items.size}")
            }
            Result.success(orders)
        } catch (e: Exception) {
            println("❌ Exception in getUserOrders: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun listenToOrderStatus(orderId: String, onStatusUpdate: (String) -> Unit) {
        db.collection("orders").document(orderId)
            .addSnapshotListener { snapshot, _ ->
                val status = snapshot?.getString("status") ?: "PENDING"
                println("📡 Order $orderId status updated to: $status")
                onStatusUpdate(status)
            }
    }

    val database: FirebaseFirestore
        get() = db
}