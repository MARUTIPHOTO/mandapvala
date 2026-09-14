package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class BillItem(
    val itemId: Long,
    val title: String,
    val category: String = "Other",
    val imageUrl: String = "",
    val quantity: Int = 1,
    val price: Double = 0.0,
    val lineTotal: Double = 0.0
)

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billNumber: String,
    val businessId: String = "biz_default",
    val customerId: Long = 0,
    val customerName: String,
    val customerMobile: String,
    val customerAddress: String = "",
    val eventDate: String = "",
    val eventType: String = "Wedding",
    val itemsJson: String = "[]",
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val gstEnabled: Boolean = false,
    val gstRate: Double = 18.0,
    val gstAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val advance: Double = 0.0,
    val balance: Double = 0.0,
    val status: String = "PENDING", // PAID, PENDING, PARTIAL
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
