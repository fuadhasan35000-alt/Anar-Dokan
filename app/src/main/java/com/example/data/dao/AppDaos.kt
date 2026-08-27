package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
    @Query("SELECT * FROM businesses WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllActiveBusinesses(): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM businesses ORDER BY createdAt DESC")
    fun getAllBusinesses(): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM businesses WHERE id = :id LIMIT 1")
    suspend fun getBusinessById(id: String): BusinessEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusiness(business: BusinessEntity)

    @Update
    suspend fun updateBusiness(business: BusinessEntity)

    @Query("SELECT COUNT(*) FROM businesses")
    suspend fun getBusinessCount(): Int
}

@Dao
interface BranchDao {
    @Query("SELECT * FROM branches WHERE businessId = :businessId ORDER BY name ASC")
    fun getBranchesByBusiness(businessId: String): Flow<List<BranchEntity>>

    @Query("SELECT * FROM branches WHERE businessId = :businessId AND status = 'ACTIVE' ORDER BY name ASC")
    fun getActiveBranches(businessId: String): Flow<List<BranchEntity>>

    @Query("SELECT * FROM branches WHERE id = :id LIMIT 1")
    suspend fun getBranchById(id: String): BranchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranch(branch: BranchEntity)

    @Update
    suspend fun updateBranch(branch: BranchEntity)

    @Query("SELECT COUNT(*) FROM branches WHERE businessId = :businessId")
    suspend fun getBranchCount(businessId: String): Int
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE businessId = :businessId ORDER BY createdAt DESC")
    fun getUsersByBusiness(businessId: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE businessId = :businessId AND status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingUsers(businessId: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET status = :status, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updateUserStatus(userId: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE users SET permissionsCsv = :permissions, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updateUserPermissions(userId: String, permissions: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM users WHERE role = 'SUPER_ADMIN'")
    suspend fun getSuperAdminCount(): Int
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE businessId = :businessId AND branchId = :branchId AND deletedAt IS NULL ORDER BY name ASC")
    fun getProductsByBranch(businessId: String, branchId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY name ASC")
    fun getProductsByBusiness(businessId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE businessId = :businessId AND branchId = :branchId AND deletedAt IS NULL AND currentStock <= minimumStock ORDER BY currentStock ASC")
    fun getLowStockProducts(businessId: String, branchId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE businessId = :businessId AND (sku = :query OR barcode = :query) AND deletedAt IS NULL LIMIT 1")
    suspend fun findBySkuOrBarcode(businessId: String, query: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET currentStock = :newStock, updatedAt = :timestamp WHERE id = :productId")
    suspend fun updateStock(productId: String, newStock: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE products SET deletedAt = :timestamp WHERE id = :productId")
    suspend fun softDeleteProduct(productId: String, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface StockTransactionDao {
    @Query("SELECT * FROM stock_transactions WHERE businessId = :businessId AND branchId = :branchId ORDER BY dateTime DESC")
    fun getTransactionsByBranch(businessId: String, branchId: String): Flow<List<StockTransactionEntity>>

    @Query("SELECT * FROM stock_transactions WHERE productId = :productId ORDER BY dateTime DESC")
    fun getTransactionsByProduct(productId: String): Flow<List<StockTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: StockTransactionEntity)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY name ASC")
    fun getCustomersByBusiness(businessId: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE businessId = :businessId AND branchId = :branchId AND deletedAt IS NULL ORDER BY name ASC")
    fun getCustomersByBranch(businessId: String, branchId: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE businessId = :businessId AND totalDue > 0 AND deletedAt IS NULL ORDER BY totalDue DESC")
    fun getCustomersWithDue(businessId: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET totalPurchase = totalPurchase + :additionalPurchase, totalPaid = totalPaid + :additionalPaid, totalDue = totalDue + :additionalDue, updatedAt = :updatedAt WHERE id = :customerId")
    suspend fun updateCustomerBalance(customerId: String, additionalPurchase: Double, additionalPaid: Double, additionalDue: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE customers SET deletedAt = :timestamp WHERE id = :customerId")
    suspend fun softDeleteCustomer(customerId: String, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales WHERE businessId = :businessId AND branchId = :branchId ORDER BY createdAt DESC")
    fun getSalesByBranch(businessId: String, branchId: String): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE businessId = :businessId ORDER BY createdAt DESC")
    fun getSalesByBusiness(businessId: String): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :saleId LIMIT 1")
    suspend fun getSaleById(saleId: String): SaleEntity?

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun getSaleItems(saleId: String): Flow<List<SaleItemEntity>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getSaleItemsDirect(saleId: String): List<SaleItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Query("SELECT COUNT(*) FROM sales WHERE businessId = :businessId")
    suspend fun getTotalSaleCount(businessId: String): Int
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE businessId = :businessId AND branchId = :branchId ORDER BY dateTime DESC")
    fun getPaymentsByBranch(businessId: String, branchId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY dateTime DESC")
    fun getPaymentsByCustomer(customerId: String): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE businessId = :businessId AND branchId = :branchId ORDER BY date DESC")
    fun getExpensesByBranch(businessId: String, branchId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE businessId = :businessId ORDER BY date DESC")
    fun getExpensesByBusiness(businessId: String): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: String)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs WHERE businessId = :businessId ORDER BY timestamp DESC LIMIT 200")
    fun getLogsByBusiness(businessId: String): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity)
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' OR status = 'FAILED' ORDER BY createdAt ASC")
    fun getPendingOperations(): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' OR status = 'FAILED' ORDER BY createdAt ASC")
    suspend fun getPendingOperationsDirect(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(operation: SyncQueueEntity)

    @Update
    suspend fun updateOperation(operation: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun clearSyncedOperations()
}

@Dao
interface ShopSettingsDao {
    @Query("SELECT * FROM shop_settings WHERE businessId = :businessId LIMIT 1")
    fun getSettingsByBusiness(businessId: String): Flow<ShopSettingsEntity?>

    @Query("SELECT * FROM shop_settings WHERE businessId = :businessId LIMIT 1")
    suspend fun getSettingsDirect(businessId: String): ShopSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: ShopSettingsEntity)
}
