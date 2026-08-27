package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entity.*
import com.example.data.model.AuditAction
import com.example.data.model.StockChangeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class CartItem(
    val product: ProductEntity,
    val quantity: Double = 1.0,
    val unitPrice: Double = product.salePrice
) {
    val subtotal: Double get() = quantity * unitPrice
}

class PosViewModel(private val db: AppDatabase) : ViewModel() {

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<CustomerEntity?>(null)
    val selectedCustomer: StateFlow<CustomerEntity?> = _selectedCustomer.asStateFlow()

    private val _discount = MutableStateFlow(0.0)
    val discount: StateFlow<Double> = _discount.asStateFlow()

    private val _paidAmount = MutableStateFlow<Double?>(null)
    val paidAmount: StateFlow<Double?> = _paidAmount.asStateFlow()

    private val _paymentMethod = MutableStateFlow("নগদ (Cash)")
    val paymentMethod: StateFlow<String> = _paymentMethod.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    val subtotal: StateFlow<Double> = _cartItems.map { items ->
        items.sumOf { it.subtotal }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val grandTotal: StateFlow<Double> = combine(subtotal, _discount) { sub, disc ->
        (sub - disc).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val dueAmount: StateFlow<Double> = combine(grandTotal, _paidAmount) { total, paid ->
        val effectivePaid = paid ?: total
        (total - effectivePaid).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(cat: String?) {
        _selectedCategory.value = cat
    }

    fun addToCart(product: ProductEntity) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val item = current[index]
            if (item.quantity + 1.0 <= product.currentStock) {
                current[index] = item.copy(quantity = item.quantity + 1.0)
            }
        } else {
            if (product.currentStock >= 1.0) {
                current.add(CartItem(product = product, quantity = 1.0, unitPrice = product.salePrice))
            }
        }
        _cartItems.value = current
    }

    fun updateQuantity(productId: String, newQty: Double) {
        if (newQty <= 0) {
            removeFromCart(productId)
            return
        }
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val item = current[index]
            if (newQty <= item.product.currentStock) {
                current[index] = item.copy(quantity = newQty)
                _cartItems.value = current
            }
        }
    }

    fun removeFromCart(productId: String) {
        _cartItems.value = _cartItems.value.filter { it.product.id != productId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _selectedCustomer.value = null
        _discount.value = 0.0
        _paidAmount.value = null
    }

    fun selectCustomer(customer: CustomerEntity?) {
        _selectedCustomer.value = customer
    }

    fun setDiscount(amount: Double) {
        _discount.value = amount.coerceAtLeast(0.0)
    }

    fun setPaidAmount(amount: Double?) {
        _paidAmount.value = amount
    }

    fun setPaymentMethod(method: String) {
        _paymentMethod.value = method
    }

    suspend fun completeSale(
        businessId: String,
        branchId: String,
        user: UserEntity
    ): Result<SaleEntity> = withContext(Dispatchers.IO) {
        val items = _cartItems.value
        if (items.isEmpty()) {
            return@withContext Result.failure(Exception("কার্টে কোনো পণ্য নেই!"))
        }

        val total = grandTotal.value
        val paid = _paidAmount.value ?: total
        val due = dueAmount.value
        val customer = _selectedCustomer.value

        val dateCode = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val randomSuffix = (1000..9999).random()
        val invoiceNumber = "AD-$dateCode-$randomSuffix"
        val saleId = UUID.randomUUID().toString()

        try {
            // 1. Create Sale Entity
            val sale = SaleEntity(
                id = saleId,
                invoiceNumber = invoiceNumber,
                businessId = businessId,
                branchId = branchId,
                customerId = customer?.id,
                customerName = customer?.name ?: "সাধারণ কাস্টমার",
                customerPhone = customer?.phone ?: "",
                subtotal = subtotal.value,
                discount = _discount.value,
                total = total,
                paid = paid,
                due = due,
                paymentMethod = _paymentMethod.value,
                status = "COMPLETED",
                userId = user.id,
                userName = user.name,
                createdAt = System.currentTimeMillis()
            )
            db.saleDao().insertSale(sale)

            // 2. Create Sale Items and Reduce Stock
            val saleItems = mutableListOf<SaleItemEntity>()
            for (item in items) {
                val saleItem = SaleItemEntity(
                    saleId = saleId,
                    productId = item.product.id,
                    productName = item.product.name,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    purchasePrice = item.product.purchasePrice,
                    subtotal = item.subtotal,
                    unit = item.product.unit
                )
                saleItems.add(saleItem)

                val newStock = (item.product.currentStock - item.quantity).coerceAtLeast(0.0)
                db.productDao().updateStock(item.product.id, newStock)

                db.stockTransactionDao().insertTransaction(
                    StockTransactionEntity(
                        productId = item.product.id,
                        productName = item.product.name,
                        businessId = businessId,
                        branchId = branchId,
                        type = StockChangeType.SALE.name,
                        quantity = item.quantity,
                        previousStock = item.product.currentStock,
                        newStock = newStock,
                        note = "বিক্রি ইনভয়েস: $invoiceNumber",
                        userId = user.id,
                        userName = user.name
                    )
                )
            }
            db.saleDao().insertSaleItems(saleItems)

            // 3. Update Customer Balance if attached
            if (customer != null) {
                db.customerDao().updateCustomerBalance(
                    customerId = customer.id,
                    additionalPurchase = total,
                    additionalPaid = paid,
                    additionalDue = due
                )

                if (paid > 0) {
                    db.paymentDao().insertPayment(
                        PaymentEntity(
                            customerId = customer.id,
                            customerName = customer.name,
                            saleId = saleId,
                            businessId = businessId,
                            branchId = branchId,
                            amount = paid,
                            paymentMethod = _paymentMethod.value,
                            note = "ইনভয়েস পরিশোধ: $invoiceNumber",
                            receivedBy = user.name
                        )
                    )
                }
            }

            // 4. Audit Log
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    userId = user.id,
                    userName = user.name,
                    businessId = businessId,
                    branchId = branchId,
                    action = AuditAction.SALE_CREATE.name,
                    recordId = saleId,
                    details = "বিক্রি সম্পন্ন: $invoiceNumber, মোট: ৳${total.toInt()}, পরিশোধ: ৳${paid.toInt()}"
                )
            )

            // Clear state on success
            clearCart()
            Result.success(sale)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
