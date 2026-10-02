package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "businesses")
data class BusinessProfile(
    @PrimaryKey val id: String = "biz_default",
    val name: String = "Shree Radhe Mandap & Decors",
    val ownerName: String = "Ramesh Bhai Patel",
    val mobile: String = "+91 98765 43210",
    val whatsapp: String = "+91 98765 43210",
    val address: String = "Near Swaminarayan Temple, Station Road",
    val city: String = "Ahmedabad, Gujarat",
    val gstNumber: String = "24ABCDE1234F1Z5",
    val email: String = "radhemandap@example.com",
    val logoUri: String = "",
    val bankName: String = "State Bank of India",
    val bankAccount: String = "30291827364",
    val bankIfsc: String = "SBIN0001234",
    val upiId: String = "radhemandap@upi",
    val termsAndConditions: String = "1. Advance 30% booking is non-refundable.\n2. Any damage or loss to decoration items will be billed separately.\n3. Final payment required before event completion.",
    val catalogUrl: String = "https://mandapvala.in",
    val isActive: Boolean = true
)
