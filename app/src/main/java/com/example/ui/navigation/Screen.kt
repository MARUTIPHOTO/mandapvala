package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Gallery : Screen("gallery")
    object Bills : Screen("bills")
    object Customers : Screen("customers")
    object Settings : Screen("settings")
    object CreateEditBill : Screen("create_edit_bill")
    object CustomerPortal : Screen("customer_portal/{customerId}/{customerName}") {
        fun createRoute(customerId: Long, customerName: String): String {
            val encodedName = java.net.URLEncoder.encode(customerName, "UTF-8")
            return "customer_portal/$customerId/$encodedName"
        }
    }
    object Notifications : Screen("notifications")
    object RegisterBusiness : Screen("register_business")
}
