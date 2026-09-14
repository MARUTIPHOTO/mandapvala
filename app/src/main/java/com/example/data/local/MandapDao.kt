package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppNotification
import com.example.data.model.Bill
import com.example.data.model.BusinessProfile
import com.example.data.model.Customer
import com.example.data.model.GalleryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface MandapDao {

    // Business Profile
    @Query("SELECT * FROM businesses WHERE isActive = 1 LIMIT 1")
    fun getActiveBusinessProfile(): Flow<BusinessProfile?>

    @Query("SELECT * FROM businesses WHERE id = :id LIMIT 1")
    suspend fun getBusinessById(id: String): BusinessProfile?

    @Query("SELECT * FROM businesses")
    fun getAllBusinesses(): Flow<List<BusinessProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBusiness(business: BusinessProfile)

    // Customers
    @Query("SELECT * FROM customers WHERE businessId = :businessId ORDER BY id DESC")
    fun getCustomers(businessId: String): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: Long)

    // Gallery Items
    @Query("SELECT * FROM gallery_items WHERE businessId = :businessId ORDER BY id DESC")
    fun getGalleryItems(businessId: String): Flow<List<GalleryItem>>

    @Query("SELECT * FROM gallery_items WHERE id = :id LIMIT 1")
    suspend fun getGalleryItemById(id: Long): GalleryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGalleryItem(item: GalleryItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGalleryItems(items: List<GalleryItem>)

    @Update
    suspend fun updateGalleryItem(item: GalleryItem)

    @Delete
    suspend fun deleteGalleryItem(item: GalleryItem)

    @Query("DELETE FROM gallery_items WHERE id = :id")
    suspend fun deleteGalleryItemById(id: Long)

    // Bills
    @Query("SELECT * FROM bills WHERE businessId = :businessId ORDER BY id DESC")
    fun getBills(businessId: String): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE customerId = :customerId ORDER BY id DESC")
    fun getBillsByCustomer(customerId: Long): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    suspend fun getBillById(id: Long): Bill?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill): Long

    @Update
    suspend fun updateBill(bill: Bill)

    @Delete
    suspend fun deleteBill(bill: Bill)

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun deleteBillById(id: Long)

    // Notifications
    @Query("SELECT * FROM notifications WHERE businessId = :businessId ORDER BY id DESC")
    fun getNotifications(businessId: String): Flow<List<AppNotification>>

    @Query("SELECT COUNT(*) FROM notifications WHERE businessId = :businessId AND isRead = 0")
    fun getUnreadNotificationCount(businessId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotification): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Long)

    @Query("UPDATE notifications SET isRead = 1 WHERE businessId = :businessId")
    suspend fun markAllNotificationsAsRead(businessId: String)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: Long)
}
