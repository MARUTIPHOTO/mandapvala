package com.example.ui.viewmodel

import android.app.Application
import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.billing.BillingManager
import com.example.billing.SubscriptionState
import com.example.data.local.MandapDatabase
import com.example.data.model.AppNotification
import com.example.data.model.Bill
import com.example.data.model.BillItem
import com.example.data.model.BusinessProfile
import com.example.data.model.Customer
import com.example.data.model.CustomerOrder
import com.example.data.model.CustomerOrderItem
import com.example.data.model.GalleryItem
import com.example.data.model.UserProfile
import com.example.data.remote.FirebaseAuthService
import com.example.data.remote.FirebaseOrderService
import com.example.data.repository.MandapRepository
import com.example.util.JsonHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MandapViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MandapRepository
    private val firebaseOrderService = FirebaseOrderService()
    private val billingManager: BillingManager = BillingManager.getInstance(application)
    val authService = FirebaseAuthService(application)
    private val activeBizId = "biz_default"

    val userProfile: StateFlow<UserProfile?> = authService.observeCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val billingState: StateFlow<SubscriptionState> = billingManager.subscriptionState

    // Evaluates whether the user's trial has expired and requires subscription
    val isAccessBlocked: StateFlow<Boolean> = combine(userProfile, billingState) { profile, billing ->
        if (profile == null) {
            false
        } else {
            val isSubscribed = billing.isSubscribed || profile.isSubscribed
            val isTrialValid = System.currentTimeMillis() <= profile.expiryDate
            !(isSubscribed || isTrialValid)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val activeBusiness: StateFlow<BusinessProfile?>
    val allBusinesses: StateFlow<List<BusinessProfile>>
    val customers: StateFlow<List<Customer>>
    val galleryItems: StateFlow<List<GalleryItem>>
    val bills: StateFlow<List<Bill>>
    val notifications: StateFlow<List<AppNotification>>
    val unreadNotificationCount: StateFlow<Int>
    val customerOrders: StateFlow<List<CustomerOrder>>

    // Search and Filter States
    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery = _customerSearchQuery.asStateFlow()

    private val _galleryCategoryFilter = MutableStateFlow("All")
    val galleryCategoryFilter = _galleryCategoryFilter.asStateFlow()

    private val _gallerySearchQuery = MutableStateFlow("")
    val gallerySearchQuery = _gallerySearchQuery.asStateFlow()

    private val _billSearchQuery = MutableStateFlow("")
    val billSearchQuery = _billSearchQuery.asStateFlow()

    private val _billStatusFilter = MutableStateFlow("All")
    val billStatusFilter = _billStatusFilter.asStateFlow()

    // Active Bill Drafting State
    private val _draftBill = MutableStateFlow<BillDraftState>(BillDraftState())
    val draftBill = _draftBill.asStateFlow()

    // Customer Portal Selection State (Customer side)
    private val _portalSelection = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val portalSelection = _portalSelection.asStateFlow()

    // Dark Theme Toggle
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme = _isDarkTheme.asStateFlow()

    init {
        val db = MandapDatabase.getDatabase(application)
        repository = MandapRepository(db.mandapDao())

        activeBusiness = repository.getActiveBusiness()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        allBusinesses = repository.getAllBusinesses()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        customers = repository.getCustomers(activeBizId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        galleryItems = repository.getGalleryItems(activeBizId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        bills = repository.getBills(activeBizId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        notifications = repository.getNotifications(activeBizId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        unreadNotificationCount = repository.getUnreadNotificationCount(activeBizId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        customerOrders = firebaseOrderService.observeCustomerOrders()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Real-time listener for incoming customer orders from catalog
        viewModelScope.launch {
            var knownOrderIds = setOf<String>()
            var isFirst = true
            customerOrders.collect { list ->
                if (isFirst) {
                    knownOrderIds = list.map { it.id }.toSet()
                    isFirst = false
                } else {
                    val freshOrders = list.filter { it.id !in knownOrderIds && it.status == "NEW" }
                    if (freshOrders.isNotEmpty()) {
                        knownOrderIds = list.map { it.id }.toSet()
                        triggerNotificationAlert()
                        freshOrders.forEach { order ->
                            val billItems = order.items.map {
                                BillItem(
                                    itemId = it.id,
                                    title = it.title,
                                    category = it.category,
                                    imageUrl = it.imageUrl,
                                    quantity = it.quantity,
                                    price = it.price,
                                    lineTotal = it.quantity * it.price
                                )
                            }
                            repository.createNotification(
                                AppNotification(
                                    businessId = activeBizId,
                                    title = "🎉 New Order: ${order.customerName}",
                                    message = "${order.customerName} (${order.customerMobile}) selected ${order.itemsCount} items (₹%,.0f) from online catalog".format(order.totalAmount),
                                    customerId = 0L,
                                    customerName = order.customerName,
                                    customerMobile = order.customerMobile,
                                    selectedItemsJson = JsonHelper.itemsToJson(billItems),
                                    isRead = false,
                                    timestamp = order.timestamp
                                )
                            )

                            // Auto-save new customer if not already in customer directory
                            val cleanName = order.customerName.trim()
                            if (cleanName.isNotBlank() && cleanName != "Customer" && cleanName != "Valued Customer") {
                                val exists = customers.value.any { it.name.equals(cleanName, ignoreCase = true) }
                                if (!exists) {
                                    repository.saveCustomer(
                                        Customer(
                                            name = cleanName,
                                            mobile = order.customerMobile.trim(),
                                            eventDate = order.eventDate.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
                                        )
                                    )
                                }
                            }

                            // Pre-fill draft bill so tapping Create Bill or Done has customer name and items ready
                            val calculatedSubtotal = if (order.totalAmount > 0.0) order.totalAmount else billItems.sumOf { it.lineTotal }
                            val draft = BillDraftState(
                                id = 0,
                                billNumber = "BILL-${SimpleDateFormat("yyMMdd-HHmm", Locale.getDefault()).format(Date())}",
                                customerId = 0L,
                                customerName = order.customerName,
                                customerMobile = order.customerMobile,
                                customerAddress = "",
                                eventDate = order.eventDate.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) },
                                eventType = "Wedding",
                                items = billItems,
                                subtotal = calculatedSubtotal,
                                discount = 0.0,
                                gstEnabled = false,
                                gstRate = 18.0,
                                gstAmount = 0.0,
                                grandTotal = calculatedSubtotal,
                                advance = 0.0,
                                balance = calculatedSubtotal,
                                status = "PENDING",
                                notes = if (order.notes.isNotBlank()) "Online Catalog: ${order.notes}" else "Customer Online Catalog Order"
                            ).recalculate()
                            _draftBill.value = draft
                        }
                    }
                }
            }
        }
        // Set default catalog URL once if empty on clean install without overriding user settings
        viewModelScope.launch {
            val biz = repository.getActiveBusiness().firstOrNull()
            if (biz != null && biz.catalogUrl.isBlank()) {
                repository.saveBusiness(biz.copy(catalogUrl = "https://marutiphoto.github.io/mandapvala/"))
            }
        }
    }

    fun isUserLoggedIn(): Boolean {
        return authService.isUserLoggedIn()
    }

    fun toggleDarkTheme(enable: Boolean) {
        _isDarkTheme.value = enable
    }

    // Google Play Billing & 90-Day Free Trial Actions
    fun launchBillingFlow(activity: Activity, productId: String, onFallback: (() -> Unit)? = null): Boolean {
        return billingManager.launchBillingFlow(activity, productId, onFallback)
    }

    fun startLocal90DayFreeTrial() {
        billingManager.startLocal90DayFreeTrial()
    }

    fun restorePurchases() {
        billingManager.queryPurchases()
    }

    fun openPlayStoreSubscriptionSettings(context: Context) {
        billingManager.openPlayStoreSubscriptionSettings(context)
    }

    fun clearBillingMessage() {
        billingManager.clearUserMessage()
    }

    // Firebase Authentication & User Trial / Referral Logic
    fun signUp(
        name: String,
        businessName: String,
        email: String,
        password: String,
        mobile: String,
        referralCode: String?,
        onResult: (Result<UserProfile>) -> Unit
    ) {
        viewModelScope.launch {
            val result = authService.signUpWithEmail(
                name = name,
                businessName = businessName,
                email = email,
                password = password,
                mobile = mobile,
                referredByCode = referralCode
            )
            // If sign-up succeeded, update local business profile name
            result.onSuccess { profile ->
                val currentBiz = activeBusiness.value
                if (currentBiz != null) {
                    updateBusinessProfile(
                        currentBiz.copy(
                            name = profile.businessName,
                            ownerName = profile.name,
                            email = profile.email,
                            mobile = profile.mobile
                        )
                    )
                }
            }
            onResult(result)
        }
    }

    fun signIn(
        email: String,
        password: String,
        onResult: (Result<UserProfile>) -> Unit
    ) {
        viewModelScope.launch {
            val result = authService.signInWithEmail(email, password)
            onResult(result)
        }
    }

    fun signInWithGoogle(
        idToken: String,
        referredByCode: String? = null,
        onResult: (Result<UserProfile>) -> Unit
    ) {
        viewModelScope.launch {
            val result = authService.signInWithGoogleIdToken(idToken, referredByCode)
            result.onSuccess { profile ->
                val currentBiz = activeBusiness.value
                if (currentBiz != null) {
                    updateBusinessProfile(
                        currentBiz.copy(
                            name = profile.businessName,
                            ownerName = profile.name,
                            email = profile.email,
                            mobile = profile.mobile
                        )
                    )
                }
            }
            onResult(result)
        }
    }

    fun quickStartTrial(
        name: String = "Mandap Partner",
        businessName: String = "Shree Mandap & Decorators",
        referralCode: String? = null,
        onResult: (Result<UserProfile>) -> Unit
    ) {
        viewModelScope.launch {
            val result = authService.quickStartTrial(name, businessName, referralCode)
            result.onSuccess { profile ->
                val currentBiz = activeBusiness.value
                if (currentBiz != null) {
                    updateBusinessProfile(
                        currentBiz.copy(
                            name = profile.businessName,
                            ownerName = profile.name,
                            email = profile.email
                        )
                    )
                }
            }
            onResult(result)
        }
    }

    fun signOut() {
        authService.signOut()
    }

    fun isUserExpired(): Boolean {
        val profile = userProfile.value ?: return false
        val isSubscribed = billingState.value.isSubscribed || profile.isSubscribed
        if (isSubscribed) return false
        return System.currentTimeMillis() > profile.expiryDate
    }

    // Business Profile Actions
    fun updateBusinessProfile(updated: BusinessProfile) {
        viewModelScope.launch {
            repository.saveBusiness(updated)
        }
    }

    fun switchBusinessProfile(business: BusinessProfile) {
        viewModelScope.launch {
            // Set this business active and others inactive
            repository.saveBusiness(business.copy(isActive = true))
        }
    }

    fun resetSampleData() {
        viewModelScope.launch {
            repository.resetWithSampleData()
        }
    }

    // Customer Actions
    fun setCustomerSearchQuery(query: String) {
        _customerSearchQuery.value = query
    }

    fun saveCustomer(customer: Customer, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = if (customer.id == 0L) {
                repository.saveCustomer(customer)
            } else {
                repository.updateCustomer(customer)
                customer.id
            }
            onSaved(id)
        }
    }

    fun deleteCustomer(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomer(id)
        }
    }

    // Gallery Actions
    fun setGalleryCategoryFilter(category: String) {
        _galleryCategoryFilter.value = category
    }

    fun setGallerySearchQuery(query: String) {
        _gallerySearchQuery.value = query
    }

    fun saveGalleryItem(item: GalleryItem) {
        viewModelScope.launch {
            if (item.id == 0L) {
                repository.saveGalleryItem(item)
            } else {
                repository.updateGalleryItem(item)
            }
        }
    }

    fun saveGalleryItems(items: List<GalleryItem>) {
        viewModelScope.launch {
            repository.saveGalleryItems(items)
        }
    }

    fun deleteGalleryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteGalleryItem(id)
        }
    }

    // Bill Drafting & Saving Actions
    fun startNewBill(customer: Customer? = null, prefilledItems: List<BillItem> = emptyList()) {
        val nextNum = "MV-${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}-${(100..999).random()}"
        _draftBill.value = BillDraftState(
            id = 0,
            billNumber = nextNum,
            customerId = customer?.id ?: 0,
            customerName = customer?.name ?: "",
            customerMobile = customer?.mobile ?: "",
            customerAddress = customer?.address ?: "",
            eventDate = customer?.eventDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            eventType = customer?.eventType ?: "Wedding",
            items = prefilledItems.toMutableList()
        ).recalculate()
    }

    fun editExistingBill(bill: Bill) {
        _draftBill.value = BillDraftState(
            id = bill.id,
            billNumber = bill.billNumber,
            customerId = bill.customerId,
            customerName = bill.customerName,
            customerMobile = bill.customerMobile,
            customerAddress = bill.customerAddress,
            eventDate = bill.eventDate,
            eventType = bill.eventType,
            items = JsonHelper.jsonToItems(bill.itemsJson).toMutableList(),
            discount = bill.discount,
            gstEnabled = bill.gstEnabled,
            gstRate = bill.gstRate,
            advance = bill.advance,
            status = bill.status,
            notes = bill.notes
        ).recalculate()
    }

    fun duplicateBill(bill: Bill) {
        val nextNum = "MV-${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}-${(100..999).random()}"
        _draftBill.value = BillDraftState(
            id = 0, // new ID
            billNumber = nextNum,
            customerId = bill.customerId,
            customerName = bill.customerName,
            customerMobile = bill.customerMobile,
            customerAddress = bill.customerAddress,
            eventDate = bill.eventDate,
            eventType = bill.eventType,
            items = JsonHelper.jsonToItems(bill.itemsJson).toMutableList(),
            discount = bill.discount,
            gstEnabled = bill.gstEnabled,
            gstRate = bill.gstRate,
            advance = 0.0,
            status = "PENDING",
            notes = "Duplicate of ${bill.billNumber}. ${bill.notes}"
        ).recalculate()
    }

    fun updateDraftCustomer(customer: Customer) {
        _draftBill.value = _draftBill.value.copy(
            customerId = customer.id,
            customerName = customer.name,
            customerMobile = customer.mobile,
            customerAddress = customer.address,
            eventDate = customer.eventDate,
            eventType = customer.eventType
        )
    }

    fun updateDraftManualDetails(
        customerName: String,
        customerMobile: String,
        customerAddress: String,
        eventDate: String,
        eventType: String,
        notes: String
    ) {
        _draftBill.value = _draftBill.value.copy(
            customerName = customerName,
            customerMobile = customerMobile,
            customerAddress = customerAddress,
            eventDate = eventDate,
            eventType = eventType,
            notes = notes
        )
    }

    fun addItemToDraft(item: GalleryItem) {
        val currentItems = _draftBill.value.items.toMutableList()
        val existingIndex = currentItems.indexOfFirst { it.itemId == item.id }
        if (existingIndex >= 0) {
            val existing = currentItems[existingIndex]
            val newQty = existing.quantity + 1
            currentItems[existingIndex] = existing.copy(
                quantity = newQty,
                lineTotal = newQty * existing.price
            )
        } else {
            currentItems.add(
                BillItem(
                    itemId = item.id,
                    title = item.title,
                    category = item.category,
                    imageUrl = item.imageUrl,
                    quantity = 1,
                    price = item.price,
                    lineTotal = item.price
                )
            )
        }
        _draftBill.value = _draftBill.value.copy(items = currentItems).recalculate()
    }

    fun updateDraftItemQuantity(index: Int, newQuantity: Int) {
        val currentItems = _draftBill.value.items.toMutableList()
        if (index in currentItems.indices) {
            if (newQuantity <= 0) {
                currentItems.removeAt(index)
            } else {
                val item = currentItems[index]
                currentItems[index] = item.copy(
                    quantity = newQuantity,
                    lineTotal = newQuantity * item.price
                )
            }
            _draftBill.value = _draftBill.value.copy(items = currentItems).recalculate()
        }
    }

    fun updateDraftItemPrice(index: Int, newPrice: Double) {
        val currentItems = _draftBill.value.items.toMutableList()
        if (index in currentItems.indices) {
            val item = currentItems[index]
            currentItems[index] = item.copy(
                price = newPrice,
                lineTotal = item.quantity * newPrice
            )
            _draftBill.value = _draftBill.value.copy(items = currentItems).recalculate()
        }
    }

    fun removeDraftItem(index: Int) {
        val currentItems = _draftBill.value.items.toMutableList()
        if (index in currentItems.indices) {
            currentItems.removeAt(index)
            _draftBill.value = _draftBill.value.copy(items = currentItems).recalculate()
        }
    }

    fun setDraftDiscount(discount: Double) {
        _draftBill.value = _draftBill.value.copy(discount = discount).recalculate()
    }

    fun setDraftGstEnabled(enabled: Boolean) {
        _draftBill.value = _draftBill.value.copy(gstEnabled = enabled).recalculate()
    }

    fun setDraftAdvance(advance: Double) {
        _draftBill.value = _draftBill.value.copy(advance = advance).recalculate()
    }

    fun setDraftStatus(status: String) {
        _draftBill.value = _draftBill.value.copy(status = status)
    }

    fun saveDraftBill(onSaved: (Bill) -> Unit) {
        val state = _draftBill.value
        val billEntity = Bill(
            id = state.id,
            billNumber = state.billNumber,
            businessId = activeBizId,
            customerId = state.customerId,
            customerName = state.customerName.ifBlank { "Walk-in Customer" },
            customerMobile = state.customerMobile,
            customerAddress = state.customerAddress,
            eventDate = state.eventDate,
            eventType = state.eventType,
            itemsJson = JsonHelper.itemsToJson(state.items),
            subtotal = state.subtotal,
            discount = state.discount,
            gstEnabled = state.gstEnabled,
            gstRate = state.gstRate,
            gstAmount = state.gstAmount,
            grandTotal = state.grandTotal,
            advance = state.advance,
            balance = state.balance,
            status = state.status,
            notes = state.notes
        )
        viewModelScope.launch {
            // Auto-save new customer if not already in customer directory
            val cleanName = state.customerName.trim()
            if (cleanName.isNotBlank() && cleanName != "Walk-in Customer") {
                val exists = customers.value.any { it.name.equals(cleanName, ignoreCase = true) }
                if (!exists) {
                    repository.saveCustomer(
                        Customer(
                            name = cleanName,
                            mobile = state.customerMobile.trim(),
                            address = state.customerAddress.trim(),
                            eventDate = state.eventDate,
                            eventType = state.eventType
                        )
                    )
                }
            }

            if (billEntity.id == 0L) {
                val newId = repository.saveBill(billEntity)
                onSaved(billEntity.copy(id = newId))
            } else {
                repository.updateBill(billEntity)
                onSaved(billEntity)
            }
        }
    }

    fun deleteBill(id: Long) {
        viewModelScope.launch {
            repository.deleteBill(id)
        }
    }

    fun setBillSearchQuery(query: String) {
        _billSearchQuery.value = query
    }

    fun setBillStatusFilter(filter: String) {
        _billStatusFilter.value = filter
    }

    // Customer Portal Simulation & Live Notification
    fun updatePortalItemQty(itemId: Long, delta: Int) {
        val current = _portalSelection.value.toMutableMap()
        val count = (current[itemId] ?: 0) + delta
        if (count <= 0) {
            current.remove(itemId)
        } else {
            current[itemId] = count
        }
        _portalSelection.value = current
    }

    /**
     * Manually set exact quantity for an item in the Customer Selection Portal
     */
    fun setPortalItemExactQty(itemId: Long, exactQty: Int) {
        val current = _portalSelection.value.toMutableMap()
        if (exactQty <= 0) {
            current.remove(itemId)
        } else {
            current[itemId] = exactQty
        }
        _portalSelection.value = current
    }

    /**
     * Allow customer or decorator to manually add a custom item and its quantity
     * directly into the selection portal.
     */
    fun addCustomPortalItem(title: String, category: String, quantity: Int, estimatedPrice: Double = 0.0) {
        viewModelScope.launch {
            val cleanTitle = title.trim()
            if (cleanTitle.isBlank()) return@launch
            val newItem = GalleryItem(
                title = cleanTitle,
                category = category.trim().ifBlank { "Other" },
                price = estimatedPrice,
                availableQuantity = quantity,
                description = "Custom customer item / requirement"
            )
            val insertedId = repository.saveGalleryItem(newItem)
            setPortalItemExactQty(insertedId, quantity)
        }
    }

    fun submitCustomerSelection(
        customerName: String,
        customerId: Long,
        customerMobile: String = "",
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val selection = _portalSelection.value
            val allItems = galleryItems.value
            val selectedBillItems = mutableListOf<BillItem>()

            selection.forEach { (itemId, qty) ->
                allItems.find { it.id == itemId }?.let { galleryItem ->
                    selectedBillItems.add(
                        BillItem(
                            itemId = galleryItem.id,
                            title = galleryItem.title,
                            category = galleryItem.category,
                            imageUrl = galleryItem.imageUrl,
                            quantity = qty,
                            price = galleryItem.price,
                            lineTotal = qty * galleryItem.price
                        )
                    )
                }
            }

            val count = selectedBillItems.sumOf { it.quantity }
            val matchedCust = customers.value.find { it.id == customerId }
                ?: customers.value.find { it.name.equals(customerName, ignoreCase = true) }
            val finalPhone = if (customerMobile.isNotBlank()) customerMobile else (matchedCust?.mobile ?: "")

            val notifMessage = if (finalPhone.isNotBlank()) {
                "$customerName ($finalPhone) selected $count items for upcoming event."
            } else {
                "$customerName selected $count items for upcoming event."
            }

            val notification = AppNotification(
                businessId = activeBizId,
                title = "🎉 New Selection from $customerName",
                message = notifMessage,
                customerId = customerId,
                customerName = customerName,
                customerMobile = finalPhone,
                selectedItemsJson = JsonHelper.itemsToJson(selectedBillItems),
                isRead = false,
                timestamp = System.currentTimeMillis()
            )

            repository.createNotification(notification)

            // Auto-save customer if not already in customer directory
            val cleanCustName = customerName.trim()
            var effectiveCustId = matchedCust?.id ?: customerId
            if (matchedCust == null && cleanCustName.isNotBlank() && cleanCustName != "Customer" && cleanCustName != "Valued Customer") {
                val exists = customers.value.any { it.name.equals(cleanCustName, ignoreCase = true) }
                if (!exists) {
                    val newCustId = repository.saveCustomer(
                        Customer(
                            name = cleanCustName,
                            mobile = finalPhone.trim(),
                            eventDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        )
                    )
                    effectiveCustId = newCustId
                }
            }

            // Pre-fill draft bill with customer name and items so Done / Create Bill works instantly
            val calculatedSubtotal = selectedBillItems.sumOf { it.lineTotal }
            val draft = BillDraftState(
                id = 0,
                billNumber = "BILL-${SimpleDateFormat("yyMMdd-HHmm", Locale.getDefault()).format(Date())}",
                customerId = effectiveCustId,
                customerName = customerName,
                customerMobile = finalPhone,
                customerAddress = matchedCust?.address ?: "",
                eventDate = matchedCust?.eventDate.takeIf { !it.isNullOrBlank() } ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                eventType = "Wedding",
                items = selectedBillItems,
                subtotal = calculatedSubtotal,
                discount = 0.0,
                gstEnabled = false,
                gstRate = 18.0,
                gstAmount = 0.0,
                grandTotal = calculatedSubtotal,
                advance = 0.0,
                balance = calculatedSubtotal,
                status = "PENDING",
                notes = "Customer Selection (${selectedBillItems.size} items)"
            ).recalculate()
            _draftBill.value = draft

            // Also push to Firestore CustomerOrders so web & in-app sync seamlessly
            val custOrder = CustomerOrder(
                customerName = customerName,
                customerMobile = finalPhone,
                eventDate = matchedCust?.eventDate ?: "",
                items = selectedBillItems.map {
                    CustomerOrderItem(
                        id = it.itemId,
                        title = it.title,
                        category = it.category,
                        price = it.price,
                        imageUrl = it.imageUrl,
                        quantity = it.quantity
                    )
                },
                totalAmount = selectedBillItems.sumOf { it.lineTotal },
                itemsCount = selectedBillItems.sumOf { it.quantity },
                status = "NEW",
                timestamp = System.currentTimeMillis(),
                notes = "In-App Customer Selection"
            )
            firebaseOrderService.submitCustomerOrder(custOrder)

            // Play notification sound & vibration cue
            triggerNotificationAlert()

            // Reset portal selection
            _portalSelection.value = emptyMap()
            onComplete()
        }
    }

    private fun triggerNotificationAlert() {
        try {
            // Beep tone
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 250)

            // Vibration
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(200)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Notification Actions
    fun markNotificationAsRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead(activeBizId)
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun convertNotificationToBill(notification: AppNotification, onReady: () -> Unit) {
        val selectedItems = JsonHelper.jsonToItems(notification.selectedItemsJson)
        val customer = customers.value.find { it.id == notification.customerId }
            ?: customers.value.find { it.name.equals(notification.customerName, ignoreCase = true) }
        val finalMobile = if (customer?.mobile?.isNotBlank() == true) customer.mobile else notification.customerMobile

        startNewBill(
            customer = customer ?: Customer(
                name = notification.customerName.ifBlank { "Customer" },
                mobile = finalMobile,
                eventDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            ),
            prefilledItems = selectedItems
        )
        markNotificationAsRead(notification.id)
        onReady()
    }

    fun convertCustomerOrderToBill(order: CustomerOrder, onReady: () -> Unit) {
        val convertedItems = order.items.map { item ->
            BillItem(
                itemId = item.id,
                title = item.title,
                category = item.category,
                imageUrl = item.imageUrl,
                quantity = item.quantity,
                price = item.price,
                lineTotal = item.quantity * item.price
            )
        }
        val calculatedSubtotal = if (order.totalAmount > 0.0) order.totalAmount else convertedItems.sumOf { it.lineTotal }
        val dateString = if (order.eventDate.isNotBlank()) order.eventDate else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val draft = BillDraftState(
            id = 0,
            billNumber = "BILL-${SimpleDateFormat("yyMMdd-HHmm", Locale.getDefault()).format(Date())}",
            customerId = 0,
            customerName = order.customerName,
            customerMobile = order.customerMobile,
            customerAddress = "",
            eventDate = dateString,
            eventType = "Wedding",
            items = convertedItems,
            subtotal = calculatedSubtotal,
            discount = 0.0,
            gstEnabled = false,
            gstRate = 18.0,
            gstAmount = 0.0,
            grandTotal = calculatedSubtotal,
            advance = 0.0,
            balance = calculatedSubtotal,
            status = "PENDING",
            notes = if (order.notes.isNotBlank()) "Online Order: ${order.notes}" else "Customer Online Catalog Order"
        ).recalculate()

        _draftBill.value = draft
        // Mark order as BILLED in Firestore
        if (order.id.isNotBlank()) {
            firebaseOrderService.updateOrderStatus(order.id, "BILLED")
        }
        onReady()
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        firebaseOrderService.updateOrderStatus(orderId, newStatus)
    }

    fun deleteCustomerOrder(orderId: String) {
        firebaseOrderService.deleteCustomerOrder(orderId)
    }
}

data class BillDraftState(
    val id: Long = 0,
    val billNumber: String = "",
    val customerId: Long = 0,
    val customerName: String = "",
    val customerMobile: String = "",
    val customerAddress: String = "",
    val eventDate: String = "",
    val eventType: String = "Wedding",
    val items: List<BillItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val gstEnabled: Boolean = false,
    val gstRate: Double = 18.0,
    val gstAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val advance: Double = 0.0,
    val balance: Double = 0.0,
    val status: String = "PENDING",
    val notes: String = ""
) {
    fun recalculate(): BillDraftState {
        val calculatedSubtotal = items.sumOf { it.lineTotal }
        val discounted = (calculatedSubtotal - discount).coerceAtLeast(0.0)
        val calculatedGst = if (gstEnabled) discounted * (gstRate / 100.0) else 0.0
        val total = discounted + calculatedGst
        val pendingBalance = (total - advance).coerceAtLeast(0.0)

        val autoStatus = when {
            total > 0 && advance >= total -> "PAID"
            advance > 0 -> "PARTIAL"
            else -> "PENDING"
        }

        return this.copy(
            subtotal = calculatedSubtotal,
            gstAmount = calculatedGst,
            grandTotal = total,
            balance = pendingBalance,
            status = autoStatus
        )
    }
}
