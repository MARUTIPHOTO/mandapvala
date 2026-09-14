package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.MandapDatabase
import com.example.data.model.AppNotification
import com.example.data.model.Bill
import com.example.data.model.BillItem
import com.example.data.model.BusinessProfile
import com.example.data.model.Customer
import com.example.data.model.GalleryItem
import com.example.data.repository.MandapRepository
import com.example.util.JsonHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MandapViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MandapRepository
    private val activeBizId = "biz_default"

    val activeBusiness: StateFlow<BusinessProfile?>
    val allBusinesses: StateFlow<List<BusinessProfile>>
    val customers: StateFlow<List<Customer>>
    val galleryItems: StateFlow<List<GalleryItem>>
    val bills: StateFlow<List<Bill>>
    val notifications: StateFlow<List<AppNotification>>
    val unreadNotificationCount: StateFlow<Int>

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
    }

    fun toggleDarkTheme(enable: Boolean) {
        _isDarkTheme.value = enable
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

    fun submitCustomerSelection(
        customerName: String,
        customerId: Long,
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
            val notification = AppNotification(
                businessId = activeBizId,
                title = "🎉 New Selection from $customerName",
                message = "$customerName selected $count items for upcoming event.",
                customerId = customerId,
                customerName = customerName,
                selectedItemsJson = JsonHelper.itemsToJson(selectedBillItems),
                isRead = false,
                timestamp = System.currentTimeMillis()
            )

            repository.createNotification(notification)

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
        startNewBill(
            customer = customer ?: Customer(
                name = notification.customerName,
                mobile = "",
                eventDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            ),
            prefilledItems = selectedItems
        )
        markNotificationAsRead(notification.id)
        onReady()
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
