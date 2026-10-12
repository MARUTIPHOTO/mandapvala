package com.example.data.model

import java.util.concurrent.TimeUnit

/**
 * Represents a registered business user in Firestore.
 * Contains 90-day free trial registration, expiry dates, and referral program data.
 */
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val businessName: String = "",
    val email: String = "",
    val mobile: String = "",
    val registeredAt: Long = System.currentTimeMillis(),
    val expiryDate: Long = System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000L),
    val referralCode: String = "",
    val referredBy: String? = null,
    val referralCount: Int = 0,
    val bonusDaysAdded: Int = 0,
    val isSubscribed: Boolean = false,
    val subscriptionProductId: String? = null
) {
    /**
     * Checks if the 90-day trial (plus any referral bonus days) is currently active.
     */
    val isTrialActive: Boolean
        get() = System.currentTimeMillis() < expiryDate

    /**
     * Total full access boolean (Active Google Play subscription OR valid 90-day trial).
     */
    val hasFullAccess: Boolean
        get() = isSubscribed || isTrialActive

    /**
     * Calculates remaining trial days.
     */
    val remainingDays: Int
        get() {
            val diff = expiryDate - System.currentTimeMillis()
            return if (diff <= 0) 0 else ((diff / (1000L * 60 * 60 * 24)).toInt() + 1)
        }

    /**
     * Formatted string of remaining days.
     */
    val remainingDaysFormatted: String
        get() = if (remainingDays > 0) "$remainingDays દિવસ બાકી" else "ટ્રાયલ સમાપ્ત"

    /**
     * Referral progress towards the next +60 days bonus (every 3 referrals = +60 days).
     */
    val referralsToNextReward: Int
        get() {
            val remainder = referralCount % 3
            return if (remainder == 0 && referralCount > 0) 0 else (3 - remainder)
        }
}

/**
 * Record of an individual referral event in Firestore.
 */
data class ReferralRecord(
    val id: String = "",
    val referrerUid: String = "",
    val referrerCode: String = "",
    val referredUid: String = "",
    val referredName: String = "",
    val referredEmail: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val rewardedBonusDays: Int = 0
)
