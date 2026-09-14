package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: String = "biz_default",
    val title: String,
    val message: String,
    val customerId: Long = 0,
    val customerName: String = "",
    val selectedItemsJson: String = "[]",
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
