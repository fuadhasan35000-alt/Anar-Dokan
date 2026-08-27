package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.*
import com.example.data.entity.*

@Database(
    entities = [
        BusinessEntity::class,
        BranchEntity::class,
        UserEntity::class,
        ProductEntity::class,
        CategoryEntity::class,
        SupplierEntity::class,
        StockTransactionEntity::class,
        CustomerEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        PaymentEntity::class,
        ExpenseEntity::class,
        AuditLogEntity::class,
        SyncQueueEntity::class,
        ShopSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao
    abstract fun branchDao(): BranchDao
    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun stockTransactionDao(): StockTransactionDao
    abstract fun customerDao(): CustomerDao
    abstract fun saleDao(): SaleDao
    abstract fun paymentDao(): PaymentDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun shopSettingsDao(): ShopSettingsDao
}

object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "amar_dokan_local.db"
            )
                .fallbackToDestructiveMigration()
                .build()
            INSTANCE = instance
            instance
        }
    }
}
