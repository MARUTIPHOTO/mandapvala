package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gallery_items")
data class GalleryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: String = "biz_default",
    val title: String,
    val category: String, // Mandap, Sofa, Chair, Flower, Lighting, Stage, Gate, Other
    val price: Double,
    val availableQuantity: Int = 1,
    val imageUrl: String = "",
    val description: String = "",
    val isAvailable: Boolean = true
)
