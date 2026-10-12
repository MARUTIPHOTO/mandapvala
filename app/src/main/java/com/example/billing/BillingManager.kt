package com.example.billing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class BillingManager private constructor(private val context: Context) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _subscriptionState = MutableStateFlow(SubscriptionState())
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    private var billingClient: BillingClient? = null
    private var cachedProductDetails: Map<String, ProductDetails> = emptyMap()
    private var connectionRetryCount = 0

    init {
        initTrialStatus()
        setupBillingClient()
    }

    /**
     * Initializes or checks the 90-day free trial on device.
     * Records the initial first launch timestamp if not already set.
     */
    private fun initTrialStatus() {
        val now = System.currentTimeMillis()
        var trialStart = prefs.getLong(KEY_TRIAL_START_TIME, 0L)
        if (trialStart == 0L) {
            trialStart = now
            prefs.edit().putLong(KEY_TRIAL_START_TIME, trialStart).apply()
        }

        val trialEnd = trialStart + SubscriptionConstants.TRIAL_DURATION_MS
        val isPurchased = prefs.getBoolean(KEY_IS_PURCHASED, false)
        val savedProductId = prefs.getString(KEY_PURCHASED_PRODUCT_ID, null)
        val savedOrderId = prefs.getString(KEY_ORDER_ID, null)
        val savedToken = prefs.getString(KEY_PURCHASE_TOKEN, null)

        val daysRemaining = if (now < trialEnd) {
            val diffMs = trialEnd - now
            TimeUnit.MILLISECONDS.toDays(diffMs).toInt().coerceAtLeast(1)
        } else {
            0
        }

        val isTrialActive = now < trialEnd

        _subscriptionState.update { current ->
            current.copy(
                isSubscribed = isPurchased,
                isTrialActive = isTrialActive,
                trialDaysRemaining = daysRemaining,
                trialStartDate = trialStart,
                trialEndDate = trialEnd,
                activeProductId = savedProductId,
                orderId = savedOrderId,
                purchaseToken = savedToken,
                availablePlans = defaultPlans()
            )
        }
    }

    /**
     * Set up Google Play BillingClient with modern 7.x lifecycle
     */
    private fun setupBillingClient() {
        try {
            val pendingParams = PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()

            billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases(pendingParams)
                .build()

            startBillingConnection()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing BillingClient: ${e.message}", e)
        }
    }

    fun startBillingConnection() {
        val client = billingClient ?: return
        if (client.isReady) {
            queryPurchases()
            queryAvailableSubscriptions()
            return
        }

        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Google Play Billing setup successful")
                    connectionRetryCount = 0
                    _subscriptionState.update { it.copy(isBillingConnected = true, userMessage = null) }
                    queryPurchases()
                    queryAvailableSubscriptions()
                } else {
                    Log.w(TAG, "Billing setup finished with code: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                    _subscriptionState.update { it.copy(isBillingConnected = false) }
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected. Reconnecting...")
                _subscriptionState.update { it.copy(isBillingConnected = false) }
                if (connectionRetryCount < 3) {
                    connectionRetryCount++
                    scope.launch {
                        delay(2000L * connectionRetryCount)
                        startBillingConnection()
                    }
                }
            }
        })
    }

    /**
     * Query existing subscription purchases from Google Play
     */
    fun queryPurchases() {
        val client = billingClient
        if (client == null || !client.isReady) {
            startBillingConnection()
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        client.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var activeSubscriptionFound = false
                var activePurchase: Purchase? = null

                for (purchase in purchasesList) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        activeSubscriptionFound = true
                        activePurchase = purchase
                        if (!purchase.isAcknowledged) {
                            acknowledgePurchase(purchase)
                        }
                    }
                }

                if (activeSubscriptionFound && activePurchase != null) {
                    val prodId = activePurchase.products.firstOrNull() ?: SubscriptionConstants.PRODUCT_SUBSCRIPTION_MAIN
                    savePurchaseLocally(true, prodId, activePurchase.orderId, activePurchase.purchaseToken)
                    _subscriptionState.update {
                        it.copy(
                            isSubscribed = true,
                            activeProductId = prodId,
                            orderId = activePurchase.orderId,
                            purchaseToken = activePurchase.purchaseToken,
                            userMessage = "Google Play સબ્સ્ક્રિપ્શન સક્રિય છે"
                        )
                    }
                } else {
                    // Re-verify local trial validity if no active Play Store subscription found
                    val now = System.currentTimeMillis()
                    val trialEnd = _subscriptionState.value.trialEndDate
                    val isTrialActive = now < trialEnd
                    val daysRemaining = if (isTrialActive) {
                        TimeUnit.MILLISECONDS.toDays(trialEnd - now).toInt().coerceAtLeast(1)
                    } else 0

                    // Keep local trial status
                    _subscriptionState.update {
                        it.copy(
                            isSubscribed = false,
                            isTrialActive = isTrialActive,
                            trialDaysRemaining = daysRemaining
                        )
                    }
                    savePurchaseLocally(false, null, null, null)
                }
            } else {
                Log.e(TAG, "queryPurchases failed: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Query available subscription products with 90-day free trial offer details
     */
    private fun queryAvailableSubscriptions() {
        val client = billingClient
        if (client == null || !client.isReady) return

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SubscriptionConstants.PRODUCT_SUBSCRIPTION_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SubscriptionConstants.PRODUCT_SUBSCRIPTION_MAIN)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        client.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val map = mutableMapOf<String, ProductDetails>()
                val plans = mutableListOf<SubscriptionPlan>()

                for (details in productDetailsList) {
                    map[details.productId] = details
                    val offers = details.subscriptionOfferDetails ?: emptyList()
                    val offer = offers.firstOrNull()
                    val formattedPrice = offer?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice ?: "₹149 / વર્ષ"
                    val offerToken = offer?.offerToken ?: ""

                    plans.add(
                        SubscriptionPlan(
                            productId = details.productId,
                            title = details.name.ifBlank { "Mandap Vala Pro - 1 Year Plan" },
                            description = details.description.ifBlank { "90 દિવસ ફ્રી ટ્રાયલ સાથે તમામ ફીચર્સ અનલિમિટેડ (₹149/વર્ષ)" },
                            formattedPrice = formattedPrice,
                            billingPeriod = "વર્ષ",
                            offerToken = offerToken,
                            hasFreeTrial = true,
                            trialDurationDays = 90,
                            trialDurationFormatted = "90 દિવસ ફ્રી ટ્રાયલ"
                        )
                    )
                }

                cachedProductDetails = map
                _subscriptionState.update { it.copy(availablePlans = plans) }
            } else {
                Log.w(TAG, "No Play Store products fetched or code: ${billingResult.responseCode}. Using standard trial plans.")
                _subscriptionState.update { it.copy(availablePlans = defaultPlans()) }
            }
        }
    }

    /**
     * Fallback plans when working offline or before Play Console subscription publish
     */
    private fun defaultPlans(): List<SubscriptionPlan> {
        return listOf(
            SubscriptionPlan(
                productId = SubscriptionConstants.PRODUCT_SUBSCRIPTION_YEARLY,
                title = "Mandap Vala Pro - 1 Year Plan (૧ વર્ષનો પ્લાન)",
                description = "90 દિવસ ફ્રી ટ્રાયલ પછી માત્ર ₹149/વર્ષ. અમર્યાદિત બુકિંગ, 1000+ ફોટો કેટલોગ અને WhatsApp બિલિંગ.",
                formattedPrice = "₹149 / વર્ષ",
                billingPeriod = "વર્ષ",
                hasFreeTrial = true,
                trialDurationDays = 90,
                trialDurationFormatted = "90 દિવસ ફ્રી ટ્રાયલ (Zero Cost Today)"
            )
        )
    }

    /**
     * Launch Google Play Billing Purchase Flow for selected subscription
     */
    fun launchBillingFlow(activity: Activity, productId: String, onFallback: (() -> Unit)? = null): Boolean {
        val client = billingClient
        val details = cachedProductDetails[productId] ?: cachedProductDetails.values.firstOrNull()

        if (client != null && client.isReady && details != null) {
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken != null) {
                val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .setOfferToken(offerToken)
                    .build()

                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productDetailsParams))
                    .build()

                val response = client.launchBillingFlow(activity, flowParams)
                if (response.responseCode == BillingClient.BillingResponseCode.OK) {
                    return true
                } else {
                    Log.e(TAG, "launchBillingFlow returned code: ${response.responseCode} - ${response.debugMessage}")
                }
            }
        }

        // If Play Billing cannot be launched (e.g. running on emulator or offline),
        // we activate the local 90-day free trial seamlessly for testing and notify user!
        onFallback?.invoke()
        return false
    }

    /**
     * Handles purchase updates from Google Play Billing
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            _subscriptionState.update { it.copy(userMessage = "સબ્સ્ક્રિપ્શન પ્રક્રિયા રદ કરવામાં આવી છે") }
        } else {
            Log.e(TAG, "onPurchasesUpdated error: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            _subscriptionState.update {
                it.copy(userMessage = "પેમેન્ટ પૂર્ણ થઈ શક્યું નથી: ${billingResult.debugMessage}")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val prodId = purchase.products.firstOrNull() ?: SubscriptionConstants.PRODUCT_SUBSCRIPTION_MAIN
            savePurchaseLocally(true, prodId, purchase.orderId, purchase.purchaseToken)

            _subscriptionState.update {
                it.copy(
                    isSubscribed = true,
                    activeProductId = prodId,
                    orderId = purchase.orderId,
                    purchaseToken = purchase.purchaseToken,
                    userMessage = "અભિનંદન! તમારો 90 દિવસનો ફ્રી ટ્રાયલ સફળતાપૂર્વક શરૂ થઈ ગયો છે."
                )
            }

            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val client = billingClient ?: return
        val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        client.acknowledgePurchase(acknowledgeParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged successfully: ${purchase.orderId}")
            } else {
                Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Start / Reset local 90-Day Free Trial (Sandbox / Direct Mode)
     */
    fun startLocal90DayFreeTrial() {
        val now = System.currentTimeMillis()
        val trialEnd = now + SubscriptionConstants.TRIAL_DURATION_MS
        prefs.edit()
            .putLong(KEY_TRIAL_START_TIME, now)
            .putBoolean(KEY_IS_PURCHASED, false)
            .apply()

        _subscriptionState.update {
            it.copy(
                isTrialActive = true,
                trialDaysRemaining = 90,
                trialStartDate = now,
                trialEndDate = trialEnd,
                userMessage = "90 દિવસનો ફ્રી ટ્રાયલ સક્રિય કરવામાં આવ્યો છે!"
            )
        }
    }

    private fun savePurchaseLocally(isPurchased: Boolean, prodId: String?, orderId: String?, token: String?) {
        prefs.edit()
            .putBoolean(KEY_IS_PURCHASED, isPurchased)
            .putString(KEY_PURCHASED_PRODUCT_ID, prodId)
            .putString(KEY_ORDER_ID, orderId)
            .putString(KEY_PURCHASE_TOKEN, token)
            .apply()
    }

    fun openPlayStoreSubscriptionSettings(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(SubscriptionConstants.PLAY_STORE_SUBSCRIPTION_URL)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open subscription url: ${e.message}")
        }
    }

    fun clearUserMessage() {
        _subscriptionState.update { it.copy(userMessage = null) }
    }

    fun destroy() {
        billingClient?.endConnection()
        billingClient = null
    }

    companion object {
        private const val TAG = "BillingManager"
        private const val PREFS_NAME = "mandap_billing_prefs"
        private const val KEY_TRIAL_START_TIME = "key_trial_start_time"
        private const val KEY_IS_PURCHASED = "key_is_purchased"
        private const val KEY_PURCHASED_PRODUCT_ID = "key_purchased_prod_id"
        private const val KEY_ORDER_ID = "key_order_id"
        private const val KEY_PURCHASE_TOKEN = "key_purchase_token"

        @Volatile
        private var instance: BillingManager? = null

        fun getInstance(context: Context): BillingManager {
            return instance ?: synchronized(this) {
                instance ?: BillingManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
