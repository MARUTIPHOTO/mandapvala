package com.example.data.repository

import com.example.data.local.MandapDao
import com.example.data.model.AppNotification
import com.example.data.model.Bill
import com.example.data.model.BusinessProfile
import com.example.data.model.Customer
import com.example.data.model.GalleryItem
import kotlinx.coroutines.flow.Flow

class MandapRepository(private val dao: MandapDao) {

    fun getActiveBusiness(): Flow<BusinessProfile?> = dao.getActiveBusinessProfile()

    fun getAllBusinesses(): Flow<List<BusinessProfile>> = dao.getAllBusinesses()

    suspend fun saveBusiness(business: BusinessProfile) = dao.insertOrUpdateBusiness(business)

    fun getCustomers(businessId: String): Flow<List<Customer>> = dao.getCustomers(businessId)

    suspend fun getCustomerById(id: Long): Customer? = dao.getCustomerById(id)

    suspend fun saveCustomer(customer: Customer): Long = dao.insertCustomer(customer)

    suspend fun updateCustomer(customer: Customer) = dao.updateCustomer(customer)

    suspend fun deleteCustomer(id: Long) = dao.deleteCustomerById(id)

    fun getGalleryItems(businessId: String): Flow<List<GalleryItem>> = dao.getGalleryItems(businessId)

    suspend fun getGalleryItemById(id: Long): GalleryItem? = dao.getGalleryItemById(id)

    suspend fun saveGalleryItem(item: GalleryItem): Long = dao.insertGalleryItem(item)

    suspend fun saveGalleryItems(items: List<GalleryItem>) = dao.insertGalleryItems(items)

    suspend fun updateGalleryItem(item: GalleryItem) = dao.updateGalleryItem(item)

    suspend fun deleteGalleryItem(id: Long) = dao.deleteGalleryItemById(id)

    fun getBills(businessId: String): Flow<List<Bill>> = dao.getBills(businessId)

    fun getBillsByCustomer(customerId: Long): Flow<List<Bill>> = dao.getBillsByCustomer(customerId)

    suspend fun getBillById(id: Long): Bill? = dao.getBillById(id)

    suspend fun saveBill(bill: Bill): Long = dao.insertBill(bill)

    suspend fun updateBill(bill: Bill) = dao.updateBill(bill)

    suspend fun deleteBill(id: Long) = dao.deleteBillById(id)

    fun getNotifications(businessId: String): Flow<List<AppNotification>> = dao.getNotifications(businessId)

    fun getUnreadNotificationCount(businessId: String): Flow<Int> = dao.getUnreadNotificationCount(businessId)

    suspend fun createNotification(notification: AppNotification): Long = dao.insertNotification(notification)

    suspend fun markNotificationAsRead(id: Long) = dao.markNotificationAsRead(id)

    suspend fun markAllNotificationsAsRead(businessId: String) = dao.markAllNotificationsAsRead(businessId)

    suspend fun deleteNotification(id: Long) = dao.deleteNotificationById(id)

    suspend fun resetWithSampleData() {
        com.example.data.local.MandapDatabase.seedInitialData(dao)
    }
}
