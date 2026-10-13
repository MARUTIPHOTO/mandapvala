package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Gallery : Screen("gallery")
    object Bills : Screen("bills")
    object Customers : Screen("customers")
    object Settings : Screen("settings")
    object CreateEditBill : Screen("create_edit_bill")
    object CustomerPortal : Screen("customer_portal?customerId={customerId}&customerName={customerName}&cname={cname}&cphone={cphone}&phone={phone}&biz={biz}") {
        fun createRoute(customerId: Long, customerName: String, customerPhone: String = ""): String {
            val encodedName = try { java.net.URLEncoder.encode(customerName, "UTF-8") } catch (e: Exception) { customerName }
            val encodedPhone = try { java.net.URLEncoder.encode(customerPhone, "UTF-8") } catch (e: Exception) { customerPhone }
            return "customer_portal?customerId=$customerId&customerName=$encodedName&cname=$encodedName&cphone=$encodedPhone"
        }
    }
    object Notifications : Screen("notifications")
    object RegisterBusiness : Screen("register_business")
    object Subscription : Screen("subscription")
    object SubscriptionRequired : Screen("subscription_required")
    object Auth : Screen("auth")
    object ReferralProgram : Screen("referral_program")
}
