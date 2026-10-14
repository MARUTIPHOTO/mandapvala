package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.UserProfile
import com.example.data.model.ReferralRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.Locale

class FirebaseAuthService(private val context: Context) {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val prefs: SharedPreferences = context.getSharedPreferences("mandap_user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "FirebaseAuthService"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_REFERRALS = "referrals"

        // 90 days in milliseconds: 90 * 24 * 60 * 60 * 1000L
        const val TRIAL_PERIOD_90_DAYS_MS = 90L * 24L * 60L * 60L * 1000L

        // 60 days bonus for every 3 referrals: 60 * 24 * 60 * 60 * 1000L
        const val BONUS_PERIOD_60_DAYS_MS = 60L * 24L * 60L * 60L * 1000L

        private const val KEY_CACHED_UID = "cached_uid"
        private const val KEY_CACHED_EXPIRY = "cached_expiry"
        private const val KEY_CACHED_REF_CODE = "cached_ref_code"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    /**
     * Checks if user has an active session or completed authentication.
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null || (prefs.getBoolean(KEY_IS_LOGGED_IN, false) && prefs.getString(KEY_CACHED_UID, null) != null)
    }

    /**
     * Observe current user profile in real-time from Firestore.
     */
    fun observeCurrentUser(): Flow<UserProfile?> = callbackFlow {
        val currentFirebaseUser = auth.currentUser
        if (currentFirebaseUser == null) {
            // Check local fallback profile if offline or guest
            val localProfile = getLocalFallbackProfile()
            trySend(localProfile)
            val authListener = FirebaseAuth.AuthStateListener { fAuth ->
                val user = fAuth.currentUser
                if (user == null) {
                    trySend(getLocalFallbackProfile())
                }
            }
            auth.addAuthStateListener(authListener)
            awaitClose { auth.removeAuthStateListener(authListener) }
            return@callbackFlow
        }

        val uid = currentFirebaseUser.uid
        val docRef = firestore.collection(COLLECTION_USERS).document(uid)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening to user doc: ${error.message}", error)
                trySend(getLocalFallbackProfile())
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java) ?: parseUserProfileFromSnapshot(snapshot)
                cacheProfileLocally(profile)
                trySend(profile)
            } else {
                // Document not yet written, use current auth data
                val fallback = createDefaultProfileForUser(currentFirebaseUser)
                trySend(fallback)
            }
        }

        awaitClose { registration.remove() }
    }

    /**
     * Signs up a new user with Email and Password, initializes 90-day trial in Firestore,
     * assigns a unique referral code, and processes the entered referral code if any.
     */
    suspend fun signUpWithEmail(
        name: String,
        businessName: String,
        email: String,
        password: String,
        mobile: String,
        referredByCode: String?
    ): Result<UserProfile> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val firebaseUser = authResult.user ?: throw Exception("User creation failed")

            val now = System.currentTimeMillis()
            val expiryDate = now + TRIAL_PERIOD_90_DAYS_MS
            val safeName = name.trim().ifBlank { "Mandap Partner" }
            val safeBizName = businessName.trim().ifBlank { "Shree Mandap & Decorators" }
            val uniqueReferralCode = generateUniqueReferralCode(safeBizName)

            val cleanReferralInput = referredByCode?.trim()?.uppercase(Locale.getDefault())

            val profile = UserProfile(
                uid = firebaseUser.uid,
                name = safeName,
                businessName = safeBizName,
                email = email.trim(),
                mobile = mobile.trim(),
                registeredAt = now,
                expiryDate = expiryDate,
                referralCode = uniqueReferralCode,
                referredBy = if (!cleanReferralInput.isNullOrBlank()) cleanReferralInput else null,
                referralCount = 0,
                bonusDaysAdded = 0,
                isSubscribed = false
            )

            // Save user to Firestore
            firestore.collection(COLLECTION_USERS)
                .document(firebaseUser.uid)
                .set(profile)
                .await()

            cacheProfileLocally(profile)

            // If a referral code was provided, process referral reward
            if (!cleanReferralInput.isNullOrBlank()) {
                processReferralCodeUsage(
                    newUserId = firebaseUser.uid,
                    newUserName = name,
                    newUserEmail = email,
                    enteredReferralCode = cleanReferralInput
                )
            }

            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "SignUp error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Signs in or registers with Google Credential (ID Token) from CredentialManager.
     * Extracts name, email from FirebaseUser, creates or loads 90-day trial user in Firestore,
     * processes referral bonus if applicable, and saves to Firestore.
     */
    suspend fun signInWithGoogleIdToken(
        idToken: String,
        referredByCode: String? = null
    ): Result<UserProfile> {
        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw Exception("Google Authentication failed")

            val userDocRef = firestore.collection(COLLECTION_USERS).document(firebaseUser.uid)
            val docSnapshot = userDocRef.get().await()

            val profile = if (docSnapshot.exists()) {
                // Existing user, load existing profile
                docSnapshot.toObject(UserProfile::class.java) ?: parseUserProfileFromSnapshot(docSnapshot)
            } else {
                // First-time user via Google Sign-In: Initialize 90-day free trial!
                val now = System.currentTimeMillis()
                val expiryDate = now + TRIAL_PERIOD_90_DAYS_MS
                val displayName = firebaseUser.displayName?.ifBlank { null } ?: "Mandap Partner"
                val email = firebaseUser.email ?: ""
                val cleanReferralInput = referredByCode?.trim()?.uppercase(Locale.getDefault())

                val newProfile = UserProfile(
                    uid = firebaseUser.uid,
                    name = displayName,
                    businessName = "${displayName} Mandap & Decorators",
                    email = email,
                    mobile = firebaseUser.phoneNumber ?: "",
                    registeredAt = now,
                    expiryDate = expiryDate,
                    referralCode = generateUniqueReferralCode(displayName),
                    referredBy = if (!cleanReferralInput.isNullOrBlank()) cleanReferralInput else null,
                    referralCount = 0,
                    bonusDaysAdded = 0,
                    isSubscribed = false
                )

                userDocRef.set(newProfile).await()

                if (!cleanReferralInput.isNullOrBlank()) {
                    processReferralCodeUsage(
                        newUserId = firebaseUser.uid,
                        newUserName = displayName,
                        newUserEmail = email,
                        enteredReferralCode = cleanReferralInput
                    )
                }

                newProfile
            }

            cacheProfileLocally(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Instant 1-click trial starter: creates a 90-day trial profile locally and in Firestore
     * so user is never blocked if Google Play Services or offline issues occur.
     */
    suspend fun quickStartTrial(
        name: String = "Mandap Partner",
        businessName: String = "Shree Mandap & Decorators",
        referralCodeInput: String? = null
    ): Result<UserProfile> {
        return try {
            val now = System.currentTimeMillis()
            val expiryDate = now + TRIAL_PERIOD_90_DAYS_MS
            val uniqueCode = generateUniqueReferralCode(businessName)
            val currentFbUser = auth.currentUser
            val uid = currentFbUser?.uid ?: "user_${now}_${(1000..9999).random()}"

            val cleanReferral = referralCodeInput?.trim()?.uppercase(Locale.getDefault())

            val profile = UserProfile(
                uid = uid,
                name = name,
                businessName = businessName,
                email = currentFbUser?.email ?: "",
                mobile = "",
                registeredAt = now,
                expiryDate = expiryDate,
                referralCode = uniqueCode,
                referredBy = if (!cleanReferral.isNullOrBlank()) cleanReferral else null,
                referralCount = 0,
                bonusDaysAdded = 0,
                isSubscribed = false
            )

            // Cache locally immediately so the app has full access
            cacheProfileLocally(profile)

            // Also try to persist in Firestore if connected
            try {
                firestore.collection(COLLECTION_USERS)
                    .document(uid)
                    .set(profile)
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore write warning during quickStartTrial: ${e.message}")
            }

            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "quickStartTrial error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Signs in with Email and Password using FirebaseAuth, with Firestore profile sync and offline fallback.
     */
    suspend fun signInWithEmail(email: String, password: String): Result<UserProfile> {
        return try {
            val cleanEmail = email.trim()
            val cleanPass = password.trim()

            // 1. Try Firebase Auth sign in
            val firebaseUser = try {
                val res = auth.signInWithEmailAndPassword(cleanEmail, cleanPass).await()
                res.user
            } catch (e: Exception) {
                null
            }

            if (firebaseUser != null) {
                val userDocRef = firestore.collection(COLLECTION_USERS).document(firebaseUser.uid)
                val doc = try { userDocRef.get().await() } catch (e: Exception) { null }
                val profile = if (doc != null && doc.exists()) {
                    doc.toObject(UserProfile::class.java) ?: parseUserProfileFromSnapshot(doc)
                } else {
                    createDefaultProfileForUser(firebaseUser)
                }
                cacheProfileLocally(profile)
                return Result.success(profile)
            }

            // 2. Query Firestore by email fallback
            val querySnapshot = try {
                firestore.collection(COLLECTION_USERS)
                    .whereEqualTo("email", cleanEmail)
                    .limit(1)
                    .get()
                    .await()
            } catch (e: Exception) {
                null
            }

            if (querySnapshot != null && !querySnapshot.isEmpty) {
                val doc = querySnapshot.documents.first()
                val profile = doc.toObject(UserProfile::class.java) ?: parseUserProfileFromSnapshot(doc)
                cacheProfileLocally(profile)
                Result.success(profile)
            } else {
                val local = getLocalFallbackProfile()
                if (local != null) {
                    Result.success(local)
                } else {
                    Result.failure(Exception("ઈમેલ અથવા પાસવર્ડ અમાન્ય છે. જો નવું એકાઉન્ટ હોય તો સાઇન અપ કરો."))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SignIn error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Signs out the current user.
     */
    fun signOut() {
        auth.signOut()
        prefs.edit().clear().apply()
    }

    /**
     * Updates user's subscription status in Firestore upon Google Play purchase confirmation.
     */
    suspend fun updateSubscriptionStatus(uid: String, isSubscribed: Boolean, productId: String?) {
        try {
            val updates = mapOf(
                "isSubscribed" to isSubscribed,
                "subscriptionProductId" to (productId ?: "")
            )
            firestore.collection(COLLECTION_USERS)
                .document(uid)
                .set(updates, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update subscription in Firestore: ${e.message}", e)
        }
    }

    /**
     * Tracks the referral when a new user registers using someone's code:
     * - Finds the referrer user in Firestore
     * - Adds record to 'referrals' collection
     * - Increments referrer's referralCount
     * - If referrer's new count hits 3 (or is a multiple of 3), adds 60 extra days to their expiryDate in Firestore!
     */
    suspend fun processReferralCodeUsage(
        newUserId: String,
        newUserName: String,
        newUserEmail: String,
        enteredReferralCode: String
    ) {
        try {
            val querySnapshot = firestore.collection(COLLECTION_USERS)
                .whereEqualTo("referralCode", enteredReferralCode)
                .limit(1)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                Log.w(TAG, "Referral code not found: $enteredReferralCode")
                return
            }

            val referrerDoc = querySnapshot.documents.first()
            val referrerUid = referrerDoc.id
            val currentCount = (referrerDoc.getLong("referralCount") ?: 0L).toInt()
            val newCount = currentCount + 1

            val currentExpiry = referrerDoc.getLong("expiryDate") ?: (System.currentTimeMillis() + TRIAL_PERIOD_90_DAYS_MS)
            val currentBonusDays = (referrerDoc.getLong("bonusDaysAdded") ?: 0L).toInt()

            var bonusGranted = 0
            var newExpiry = currentExpiry

            // If a user successfully brings 3 new installs/referrals, add 60 extra days to their expiry date in Firestore!
            if (newCount % 3 == 0) {
                bonusGranted = 60
                val base = maxOf(currentExpiry, System.currentTimeMillis())
                newExpiry = base + BONUS_PERIOD_60_DAYS_MS
                Log.i(TAG, "Referrer $referrerUid earned 60 bonus days! New count: $newCount, New expiry: $newExpiry")
            }

            // Record in 'referrals' collection
            val referralRecord = ReferralRecord(
                id = "${referrerUid}_${newUserId}",
                referrerUid = referrerUid,
                referrerCode = enteredReferralCode,
                referredUid = newUserId,
                referredName = newUserName,
                referredEmail = newUserEmail,
                timestamp = System.currentTimeMillis(),
                rewardedBonusDays = bonusGranted
            )

            firestore.collection(COLLECTION_REFERRALS)
                .document(referralRecord.id)
                .set(referralRecord)
                .await()

            // Update referrer's user document in Firestore
            val referrerUpdates = mutableMapOf<String, Any>(
                "referralCount" to newCount
            )
            if (bonusGranted > 0) {
                referrerUpdates["expiryDate"] = newExpiry
                referrerUpdates["bonusDaysAdded"] = currentBonusDays + 60
            }

            firestore.collection(COLLECTION_USERS)
                .document(referrerUid)
                .update(referrerUpdates)
                .await()

        } catch (e: Exception) {
            Log.e(TAG, "Error processing referral: ${e.message}", e)
        }
    }

    /**
     * Checks if the user is expired right now.
     */
    fun isUserExpired(profile: UserProfile?): Boolean {
        if (profile == null) return false
        if (profile.isSubscribed) return false
        return System.currentTimeMillis() > profile.expiryDate
    }

    /**
     * Generates a unique 6-character referral code (e.g., MV-8X2Q or MANDAP-4K9L).
     */
    private fun generateUniqueReferralCode(businessName: String): String {
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        val random = SecureRandom()
        val randomSuffix = (1..4)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")

        val prefix = businessName.trim().filter { it.isLetter() }.take(2).uppercase(Locale.getDefault())
        val finalPrefix = if (prefix.length == 2) prefix else "MV"
        return "$finalPrefix-$randomSuffix"
    }

    private fun parseUserProfileFromSnapshot(doc: com.google.firebase.firestore.DocumentSnapshot): UserProfile {
        val now = System.currentTimeMillis()
        val reg = doc.getLong("registeredAt") ?: now
        val exp = doc.getLong("expiryDate") ?: (reg + TRIAL_PERIOD_90_DAYS_MS)
        val count = (doc.getLong("referralCount") ?: 0L).toInt()
        val bonus = (doc.getLong("bonusDaysAdded") ?: 0L).toInt()
        val sub = doc.getBoolean("isSubscribed") ?: false

        return UserProfile(
            uid = doc.id,
            name = doc.getString("name") ?: "Mandap Owner",
            businessName = doc.getString("businessName") ?: "My Mandap & Decoration",
            email = doc.getString("email") ?: "",
            mobile = doc.getString("mobile") ?: "",
            registeredAt = reg,
            expiryDate = exp,
            referralCode = doc.getString("referralCode") ?: "MV-9090",
            referredBy = doc.getString("referredBy"),
            referralCount = count,
            bonusDaysAdded = bonus,
            isSubscribed = sub,
            subscriptionProductId = doc.getString("subscriptionProductId")
        )
    }

    private fun createDefaultProfileForUser(firebaseUser: FirebaseUser): UserProfile {
        val now = System.currentTimeMillis()
        val email = firebaseUser.email ?: "user@example.com"
        val displayName = firebaseUser.displayName ?: "Mandap Partner"
        val code = generateUniqueReferralCode(displayName)

        return UserProfile(
            uid = firebaseUser.uid,
            name = displayName,
            businessName = "Shree Mandap & Decorators",
            email = email,
            mobile = firebaseUser.phoneNumber ?: "",
            registeredAt = now,
            expiryDate = now + TRIAL_PERIOD_90_DAYS_MS,
            referralCode = code,
            referredBy = null,
            referralCount = 0,
            bonusDaysAdded = 0,
            isSubscribed = false
        )
    }

    private fun cacheProfileLocally(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_CACHED_UID, profile.uid)
            .putLong(KEY_CACHED_EXPIRY, profile.expiryDate)
            .putString(KEY_CACHED_REF_CODE, profile.referralCode)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    private fun getLocalFallbackProfile(): UserProfile? {
        val cachedUid = prefs.getString(KEY_CACHED_UID, null)
        val cachedExpiry = prefs.getLong(KEY_CACHED_EXPIRY, 0L)
        val cachedRef = prefs.getString(KEY_CACHED_REF_CODE, null)

        if (cachedUid != null && cachedExpiry > 0L) {
            return UserProfile(
                uid = cachedUid,
                name = "Mandap Partner",
                businessName = "Mandap & Decoration",
                email = "",
                registeredAt = cachedExpiry - TRIAL_PERIOD_90_DAYS_MS,
                expiryDate = cachedExpiry,
                referralCode = cachedRef ?: "MV-FREE90",
                referralCount = 0,
                bonusDaysAdded = 0,
                isSubscribed = false
            )
        }
        return null
    }
}
