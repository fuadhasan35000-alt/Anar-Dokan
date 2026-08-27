package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entity.AuditLogEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.StockTransactionEntity
import com.example.data.model.AuditAction
import com.example.data.model.StockChangeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ProductViewModel(private val db: AppDatabase) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _filterLowStockOnly = MutableStateFlow(false)
    val filterLowStockOnly: StateFlow<Boolean> = _filterLowStockOnly.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(cat: String?) {
        _selectedCategory.value = cat
    }

    fun toggleLowStockFilter() {
        _filterLowStockOnly.value = !_filterLowStockOnly.value
    }

    fun getProducts(businessId: String, branchId: String): Flow<List<ProductEntity>> {
        return db.productDao().getProductsByBranch(businessId, branchId).combine(
            combine(_searchQuery, _selectedCategory, _filterLowStockOnly) { query, cat, lowStock ->
                Triple(query, cat, lowStock)
            }
        ) { list, (query, cat, lowStock) ->
            list.filter { p ->
                val matchesQuery = query.isEmpty() ||
                        p.name.contains(query, ignoreCase = true) ||
                        p.sku.contains(query, ignoreCase = true) ||
                        p.barcode.contains(query, ignoreCase = true)

                val matchesCat = cat == null || p.category == cat
                val matchesLowStock = !lowStock || (p.currentStock <= p.minimumStock)

                matchesQuery && matchesCat && matchesLowStock
            }
        }
    }

    fun getStockTransactions(businessId: String, branchId: String): Flow<List<StockTransactionEntity>> {
        return db.stockTransactionDao().getTransactionsByBranch(businessId, branchId)
    }

    suspend fun addOrUpdateProduct(
        product: ProductEntity,
        userId: String,
        userName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val isNew = db.productDao().getProductById(product.id) == null
            db.productDao().insertProduct(product)

            db.auditLogDao().insertLog(
                AuditLogEntity(
                    userId = userId,
                    userName = userName,
                    businessId = product.businessId,
                    branchId = product.branchId,
                    action = if (isNew) AuditAction.PRODUCT_CREATE.name else AuditAction.PRODUCT_UPDATE.name,
                    recordId = product.id,
                    details = "পণ্য ${if (isNew) "যোগ করা হয়েছে" else "আপডেট করা হয়েছে"}: ${product.name} (স্টক: ${product.currentStock})"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adjustStock(
        product: ProductEntity,
        type: StockChangeType,
        quantity: Double,
        note: String,
        userId: String,
        userName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val previousStock = product.currentStock
            val newStock = when (type) {
                StockChangeType.STOCK_IN -> previousStock + quantity
                StockChangeType.STOCK_OUT -> (previousStock - quantity).coerceAtLeast(0.0)
                StockChangeType.ADJUSTMENT -> quantity.coerceAtLeast(0.0)
                StockChangeType.SALE -> (previousStock - quantity).coerceAtLeast(0.0)
                StockChangeType.RETURN -> previousStock + quantity
            }

            db.productDao().updateStock(product.id, newStock)

            db.stockTransactionDao().insertTransaction(
                StockTransactionEntity(
                    productId = product.id,
                    productName = product.name,
                    businessId = product.businessId,
                    branchId = product.branchId,
                    type = type.name,
                    quantity = quantity,
                    previousStock = previousStock,
                    newStock = newStock,
                    note = note,
                    userId = userId,
                    userName = userName
                )
            )

            db.auditLogDao().insertLog(
                AuditLogEntity(
                    userId = userId,
                    userName = userName,
                    businessId = product.businessId,
                    branchId = product.branchId,
                    action = when (type) {
                        StockChangeType.STOCK_IN -> AuditAction.STOCK_IN.name
                        StockChangeType.STOCK_OUT -> AuditAction.STOCK_OUT.name
                        else -> AuditAction.STOCK_ADJUSTMENT.name
                    },
                    recordId = product.id,
                    details = "${product.name} এ ${type.banglaTitle}: $quantity ${product.unit} (পূর্বের স্টক: $previousStock -> বর্তমান: $newStock)"
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(
        product: ProductEntity,
        userId: String,
        userName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.productDao().softDeleteProduct(product.id)
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    userId = userId,
                    userName = userName,
                    businessId = product.businessId,
                    branchId = product.branchId,
                    action = AuditAction.PRODUCT_DELETE.name,
                    recordId = product.id,
                    details = "পণ্য মুছে ফেলা হয়েছে: ${product.name}"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
