package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "businesses")
data class BusinessEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val ownerName: String,
    val phone: String,
    val address: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED"
)

@Entity(tableName = "branches")
data class BranchEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessId: String,
    val name: String,
    val address: String,
    val phone: String,
    val managerId: String? = null,
    val status: String = "ACTIVE", // ACTIVE, DISABLED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED"
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessId: String,
    val branchId: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: String, // SUPER_ADMIN, ADMIN, STAFF
    val status: String, // PENDING, APPROVED, REJECTED, DISABLED
    val permissionsCsv: String = "", // Comma separated permissions
    val passwordHash: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED"
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessId: String,
    val branchId: String,
    val name: String,
    val sku: String = "",
    val barcode: String = "",
    val category: String = "সাধারণ",
    val purchasePrice: Double = 0.0,
    val salePrice: Double = 0.0,
    val currentStock: Double = 0.0,
    val minimumStock: Double = 5.0,
    val unit: String = "পিস",
    val supplier: String = "",
    val imageUri: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = "SYNCED"
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessId: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED"
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessId: String,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val syncStatus: String = "SYNCED"
)

@Entity(tableName = "stock_transactions")
data class StockTransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val productName: String,
    val businessId: String,
    val branchId: String,
    val type: String, // STOCK_IN, STOCK_OUT, ADJUSTMENT, SALE, RETURN
    val quantity: Double,
    val previousStock: Double,
    val newStock: Double,
    val note: String = "",
    val userId: String = "",
    val userName: String = "",
    val dateTime: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED"
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessId: String,
    val branchId: String,
    val name: String,
    val phone: String,
    val address: String = "",
    val email: String = "",
    val notes: String = "",
    val totalPurchase: Double = 0.0,
    val totalPaid: Double = 0.0,
    val totalDue: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = "SYNCED"
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val invoiceNumber: String,
    val businessId: String,
    val branchId: String,
    val customerId: String? = null,
    val customerName: String = "সাধারণ কাস্টমার",
    val customerPhone: String = "",
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0,
    val paid: Double = 0.0,
    val due: Double = 0.0,
    val paymentMethod: String = "নগদ (Cash)",
    val status: String = "COMPLETED",
    val userId: String = "",
    val userName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED"
)

@Entity(tableName = "sale_items")
data class SaleItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val saleId: String,
    val productId: String,
    val productName: String,
    val quantity: Double,
    val unitPrice: Double,
    val purchasePrice: Double,
    val subtotal: Double,
    val unit: String = "পিস",
    val syncStatus: String = "SYNCED"
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val customerName: String,
    val saleId: String? = null,
    val businessId: String,
    val branchId: String,
    val amount: Double,
    val paymentMethod: String = "নগদ (Cash)",
    val note: String = "",
    val receivedBy: String = "",
    val dateTime: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED"
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessId: String,
    val branchId: String,
    val title: String,
    val category: String,
    val amount: Double,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val syncStatus: String = "SYNCED"
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val userName: String,
    val businessId: String,
    val branchId: String,
    val action: String,
    val recordId: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED"
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessId: String,
    val branchId: String,
    val entityType: String,
    val entityId: String,
    val operation: String, // INSERT, UPDATE, DELETE
    val payloadJson: String,
    val status: String = "PENDING", // PENDING, SYNCING, SYNCED, FAILED
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "shop_settings")
data class ShopSettingsEntity(
    @PrimaryKey val id: String = "default_settings",
    val businessId: String,
    val currencySymbol: String = "৳",
    val invoicePrefix: String = "AD-",
    val lowStockThreshold: Double = 5.0,
    val enableAi: Boolean = true,
    val aiApiEndpoint: String = "",
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val smsTemplate: String = "প্রিয় {name}, আপনার বর্তমান বকেয়া {amount} টাকা। - {shop}",
    val whatsappTemplate: String = "প্রিয় {name},\nআপনার {shop} থেকে ক্রয়কৃত মেমো নং {invoice}। মোট: {total} টাকা, পরিশোধ: {paid} টাকা, বকেয়া: {due} টাকা। ধন্যবাদ!"
)
