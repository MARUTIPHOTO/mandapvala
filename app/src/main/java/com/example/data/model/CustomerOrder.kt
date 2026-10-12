package com.example.data.model

data class CustomerOrder(
    val id: String = "",
    val customerName: String = "",
    val customerMobile: String = "",
    val eventDate: String = "",
    val items: List<CustomerOrderItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val itemsCount: Int = 0,
    val status: String = "NEW", // NEW, ACCEPTED, BILLED, CANCELLED
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

data class CustomerOrderItem(
    val id: Long = 0,
    val title: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val quantity: Int = 1
)
