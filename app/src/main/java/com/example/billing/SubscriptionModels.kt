package com.example.billing

/**
 * Information about a subscription plan retrieved from Google Play Billing or default fallback
 */
data class SubscriptionPlan(
    val productId: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val billingPeriod: String, // e.g. "Monthly" or "Yearly"
    val offerToken: String = "",
    val hasFreeTrial: Boolean = true,
    val trialDurationDays: Int = 90,
    val trialDurationFormatted: String = "90 દિવસ ફ્રી ટ્રાયલ (3 મહિના)"
)

/**
 * Current subscription & trial status of the user
 */
data class SubscriptionState(
    val isSubscribed: Boolean = false,
    val isTrialActive: Boolean = true,
    val trialDaysRemaining: Int = 90,
    val trialStartDate: Long = 0L,
    val trialEndDate: Long = 0L,
    val activeProductId: String? = null,
    val orderId: String? = null,
    val purchaseToken: String? = null,
    val availablePlans: List<SubscriptionPlan> = emptyList(),
    val isLoading: Boolean = false,
    val isBillingConnected: Boolean = false,
    val userMessage: String? = null
) {
    /**
     * Determines whether the user has full access to the app's features
     * (either an active Google Play subscription OR an active 90-day free trial).
     */
    val hasAccess: Boolean
        get() = isSubscribed || isTrialActive
}

object SubscriptionConstants {
    // Primary subscription product ID configured in Google Play Console (1 Year Plan for ₹149)
    const val PRODUCT_SUBSCRIPTION_YEARLY = "mandap_pro_yearly"
    const val PRODUCT_SUBSCRIPTION_MAIN = "mandap_pro_subscription"

    // 90 Days Free Trial duration in milliseconds (90 * 24 * 60 * 60 * 1000L)
    const val TRIAL_DURATION_DAYS = 90
    const val TRIAL_DURATION_MS = TRIAL_DURATION_DAYS * 24L * 60L * 60L * 1000L

    // Google Play Store subscription management URL
    const val PLAY_STORE_SUBSCRIPTION_URL = "https://play.google.com/store/account/subscriptions"
}
