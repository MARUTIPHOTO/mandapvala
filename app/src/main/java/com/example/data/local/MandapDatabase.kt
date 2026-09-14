package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AppNotification
import com.example.data.model.Bill
import com.example.data.model.BusinessProfile
import com.example.data.model.Customer
import com.example.data.model.GalleryItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        BusinessProfile::class,
        Customer::class,
        GalleryItem::class,
        Bill::class,
        AppNotification::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MandapDatabase : RoomDatabase() {

    abstract fun mandapDao(): MandapDao

    companion object {
        @Volatile
        private var INSTANCE: MandapDatabase? = null

        fun getDatabase(context: Context): MandapDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MandapDatabase::class.java,
                    "mandap_vala_database"
                )
                    .addCallback(DatabaseSeedCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseSeedCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed initial sample data asynchronously
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedInitialData(database.mandapDao())
                    }
                }
            }
        }

        suspend fun seedInitialData(dao: MandapDao) {
            val defaultBiz = BusinessProfile(
                id = "biz_default",
                name = "Radhe Mandap & Event Decors",
                ownerName = "Ramesh Bhai Patel",
                mobile = "+91 98765 43210",
                whatsapp = "+91 98765 43210",
                address = "Near Swaminarayan Temple, Station Road",
                city = "Ahmedabad, Gujarat",
                gstNumber = "24ABCDE1234F1Z5",
                email = "radhemandap@example.com",
                bankName = "State Bank of India",
                bankAccount = "30291827364",
                bankIfsc = "SBIN0001234",
                upiId = "radhemandap@upi",
                isActive = true
            )
            dao.insertOrUpdateBusiness(defaultBiz)

            val initialGallery = listOf(
                GalleryItem(
                    title = "Royal Rajwada Wedding Mandap",
                    category = "Mandap",
                    price = 45000.0,
                    availableQuantity = 2,
                    description = "Magnificent hand-carved golden pillars with fresh red roses, marigold strings & velvet dome canopy."
                ),
                GalleryItem(
                    title = "Maharaja Velvet High-Back Sofa",
                    category = "Sofa",
                    price = 6000.0,
                    availableQuantity = 4,
                    description = "Gold-gilded royal throne seating for bride & groom with embroidered velvet cushions."
                ),
                GalleryItem(
                    title = "Golden Chiavari Chairs (Pack of 50)",
                    category = "Chair",
                    price = 5000.0,
                    availableQuantity = 10,
                    description = "Premium metallic gold chiavari banquet chairs with ivory satin ties."
                ),
                GalleryItem(
                    title = "Fresh Orchid & Marigold Entrance Arch",
                    category = "Flower",
                    price = 15000.0,
                    availableQuantity = 3,
                    description = "Exotic fresh flower floral arch with white hydrangeas, orchids, and traditional marigolds."
                ),
                GalleryItem(
                    title = "LED Par & Moving Head Lighting Rig",
                    category = "Lighting",
                    price = 12000.0,
                    availableQuantity = 5,
                    description = "Color-changing ambient LED pars, warm uplighting, and sharp stage spot movers."
                ),
                GalleryItem(
                    title = "Grand Floral Pillar Reception Stage",
                    category = "Stage",
                    price = 35000.0,
                    availableQuantity = 2,
                    description = "30ft wide panoramic stage with 3D backdrop panels, floral chandeliers & carpeted steps."
                ),
                GalleryItem(
                    title = "Peacock Feather Royal Welcome Gate",
                    category = "Gate",
                    price = 18000.0,
                    availableQuantity = 2,
                    description = "Grand 16ft tall royal welcome gateway with bell decor, hanging lamps & floral cascades."
                ),
                GalleryItem(
                    title = "Heavy Brass Hanging Diyas & Bells",
                    category = "Other",
                    price = 4500.0,
                    availableQuantity = 8,
                    description = "Traditional vintage brass hanging bells and oil diyas for holy spiritual mandap aura."
                )
            )
            dao.insertGalleryItems(initialGallery)

            val cust1 = Customer(
                name = "Kunal & Priya Sharma",
                mobile = "+91 98250 11223",
                eventDate = "2026-11-15",
                eventType = "Wedding",
                address = "Shubh Banquet Lawns, SG Highway, Ahmedabad",
                notes = "Grand wedding ceremony with 800 guests. Wants Rajwada mandap."
            )
            val cust2 = Customer(
                name = "Hardik Shah",
                mobile = "+91 97240 44556",
                eventDate = "2026-11-20",
                eventType = "Reception",
                address = "Greenwoods Resort, Gandhinagar",
                notes = "Reception stage setup with high-end LED lighting."
            )
            val c1Id = dao.insertCustomer(cust1)
            val c2Id = dao.insertCustomer(cust2)

            // Seed a sample bill
            val itemsJson = """
                [
                  {"itemId":1,"title":"Royal Rajwada Wedding Mandap","category":"Mandap","quantity":1,"price":45000.0,"lineTotal":45000.0},
                  {"itemId":2,"title":"Maharaja Velvet High-Back Sofa","category":"Sofa","quantity":1,"price":6000.0,"lineTotal":6000.0},
                  {"itemId":4,"title":"Fresh Orchid & Marigold Entrance Arch","category":"Flower","quantity":1,"price":15000.0,"lineTotal":15000.0}
                ]
            """.trimIndent()
            val sampleBill = Bill(
                billNumber = "MV-2026-001",
                businessId = "biz_default",
                customerId = c1Id,
                customerName = "Kunal & Priya Sharma",
                customerMobile = "+91 98250 11223",
                customerAddress = "Shubh Banquet Lawns, SG Highway, Ahmedabad",
                eventDate = "2026-11-15",
                eventType = "Wedding",
                itemsJson = itemsJson,
                subtotal = 66000.0,
                discount = 3000.0,
                gstEnabled = true,
                gstRate = 18.0,
                gstAmount = 11340.0,
                grandTotal = 74340.0,
                advance = 30000.0,
                balance = 44340.0,
                status = "PARTIAL",
                notes = "Advance 30k received via Google Pay. Balance due 2 days before event."
            )
            dao.insertBill(sampleBill)

            // Seed a notification
            val notification = AppNotification(
                businessId = "biz_default",
                title = "New Customer Selection Received!",
                message = "Hardik Shah selected 3 items via Customer Gallery Link.",
                customerId = c2Id,
                customerName = "Hardik Shah",
                selectedItemsJson = """
                    [
                      {"itemId":6,"title":"Grand Floral Pillar Reception Stage","quantity":1,"price":35000.0},
                      {"itemId":5,"title":"LED Par & Moving Head Lighting Rig","quantity":2,"price":12000.0}
                    ]
                """.trimIndent(),
                isRead = false
            )
            dao.insertNotification(notification)
        }
    }
}
