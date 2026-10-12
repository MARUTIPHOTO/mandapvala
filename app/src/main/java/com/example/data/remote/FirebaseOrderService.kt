package com.example.data.remote

import android.util.Log
import com.example.data.model.CustomerOrder
import com.example.data.model.CustomerOrderItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirebaseOrderService {

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    companion object {
        private const val TAG = "FirebaseOrderService"
        private const val COLLECTION_ORDERS = "CustomerOrders"
    }

    fun observeCustomerOrders(): Flow<List<CustomerOrder>> = callbackFlow {
        Log.d(TAG, "Starting real-time listener on $COLLECTION_ORDERS")
        val registration = firestore.collection(COLLECTION_ORDERS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to CustomerOrders: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val orders = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.id
                        val name = doc.getString("customerName") ?: ""
                        val mobile = doc.getString("customerMobile") ?: ""
                        val date = doc.getString("eventDate") ?: ""
                        val status = doc.getString("status") ?: "NEW"
                        val notes = doc.getString("notes") ?: ""
                        
                        val total = when (val t = doc.get("totalAmount")) {
                            is Number -> t.toDouble()
                            is String -> t.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }

                        val count = when (val c = doc.get("itemsCount")) {
                            is Number -> c.toInt()
                            is String -> c.toIntOrNull() ?: 0
                            else -> 0
                        }

                        val time = when (val ts = doc.get("createdAt")) {
                            is Timestamp -> ts.toDate().time
                            is Number -> ts.toLong()
                            else -> {
                                when (val rawTime = doc.get("timestamp")) {
                                    is Number -> rawTime.toLong()
                                    else -> System.currentTimeMillis()
                                }
                            }
                        }

                        val rawItems = doc.get("items") as? List<*> ?: emptyList<Any>()
                        val items = rawItems.mapNotNull { itemObj ->
                            if (itemObj is Map<*, *>) {
                                val itemId = (itemObj["id"] as? Number)?.toLong() ?: 0L
                                val title = itemObj["title"] as? String ?: ""
                                val category = itemObj["category"] as? String ?: ""
                                val price = (itemObj["price"] as? Number)?.toDouble() ?: 0.0
                                val img = (itemObj["imageUrl"] as? String) ?: (itemObj["img"] as? String) ?: ""
                                val qty = (itemObj["quantity"] as? Number)?.toInt() ?: 1
                                CustomerOrderItem(
                                    id = itemId,
                                    title = title,
                                    category = category,
                                    price = price,
                                    imageUrl = img,
                                    quantity = qty
                                )
                            } else null
                        }

                        CustomerOrder(
                            id = id,
                            customerName = name,
                            customerMobile = mobile,
                            eventDate = date,
                            items = items,
                            totalAmount = total,
                            itemsCount = if (count > 0) count else items.sumOf { it.quantity },
                            status = status,
                            timestamp = time,
                            notes = notes
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing order doc ${doc.id}: ${e.message}", e)
                        null
                    }
                }.sortedByDescending { it.timestamp }

                trySend(orders)
            }

        awaitClose {
            Log.d(TAG, "Closing real-time listener on $COLLECTION_ORDERS")
            registration.remove()
        }
    }

    fun submitCustomerOrder(
        order: CustomerOrder,
        onSuccess: (String) -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val data = hashMapOf(
            "customerName" to order.customerName,
            "customerMobile" to order.customerMobile,
            "eventDate" to order.eventDate,
            "totalAmount" to order.totalAmount,
            "itemsCount" to order.itemsCount,
            "status" to order.status,
            "timestamp" to System.currentTimeMillis(),
            "notes" to order.notes,
            "items" to order.items.map {
                hashMapOf(
                    "id" to it.id,
                    "title" to it.title,
                    "category" to it.category,
                    "price" to it.price,
                    "imageUrl" to it.imageUrl,
                    "quantity" to it.quantity
                )
            }
        )

        firestore.collection(COLLECTION_ORDERS)
            .add(data)
            .addOnSuccessListener { ref ->
                Log.d(TAG, "CustomerOrder saved with id: ${ref.id}")
                onSuccess(ref.id)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save CustomerOrder: ${e.message}", e)
                onError(e)
            }
    }

    fun updateOrderStatus(
        orderId: String,
        newStatus: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (orderId.isBlank()) return
        firestore.collection(COLLECTION_ORDERS)
            .document(orderId)
            .update("status", newStatus)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update order status for $orderId: ${e.message}", e)
                onComplete(false)
            }
    }

    fun deleteCustomerOrder(orderId: String, onComplete: (Boolean) -> Unit = {}) {
        if (orderId.isBlank()) return
        firestore.collection(COLLECTION_ORDERS)
            .document(orderId)
            .delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}
