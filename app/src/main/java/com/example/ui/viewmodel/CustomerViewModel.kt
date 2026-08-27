package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entity.AuditLogEntity
import com.example.data.entity.CustomerEntity
import com.example.data.entity.PaymentEntity
import com.example.data.model.AuditAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.util.UUID

class CustomerViewModel(private val db: AppDatabase) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterDueOnly = MutableStateFlow(false)
    val filterDueOnly: StateFlow<Boolean> = _filterDueOnly.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleDueOnly() {
        _filterDueOnly.value = !_filterDueOnly.value
    }

    fun getCustomers(businessId: String): Flow<List<CustomerEntity>> {
        return db.customerDao().getCustomersByBusiness(businessId).combine(
            combine(_searchQuery, _filterDueOnly) { query, dueOnly ->
                Pair(query, dueOnly)
            }
        ) { list, (query, dueOnly) ->
            list.filter { c ->
                val matchesQuery = query.isEmpty() ||
                        c.name.contains(query, ignoreCase = true) ||
                        c.phone.contains(query, ignoreCase = true) ||
                        c.address.contains(query, ignoreCase = true)
                val matchesDue = !dueOnly || c.totalDue > 0
                matchesQuery && matchesDue
            }
        }
    }

    fun getCustomerPayments(customerId: String): Flow<List<PaymentEntity>> {
        return db.paymentDao().getPaymentsByCustomer(customerId)
    }

    suspend fun saveCustomer(
        customer: CustomerEntity,
        userId: String,
        userName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val isNew = db.customerDao().getCustomerById(customer.id) == null
            db.customerDao().insertCustomer(customer)
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    userId = userId,
                    userName = userName,
                    businessId = customer.businessId,
                    branchId = customer.branchId,
                    action = if (isNew) "CUSTOMER_CREATE" else "CUSTOMER_UPDATE",
                    recordId = customer.id,
                    details = "কাস্টমার ${if (isNew) "যোগ করা হয়েছে" else "আপডেট করা হয়েছে"}: ${customer.name} (মোবাইল: ${customer.phone})"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordDuePayment(
        customer: CustomerEntity,
        amount: Double,
        paymentMethod: String,
        note: String,
        userId: String,
        userName: String,
        businessId: String,
        branchId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (amount <= 0) {
            return@withContext Result.failure(Exception("পেমেন্ট টাকার পরিমাণ ০ এর বেশি হতে হবে!"))
        }

        try {
            // 1. Record payment
            val payment = PaymentEntity(
                customerId = customer.id,
                customerName = customer.name,
                businessId = businessId,
                branchId = branchId,
                amount = amount,
                paymentMethod = paymentMethod,
                note = note.ifEmpty { "বকেয়া আদায়" },
                receivedBy = userName,
                dateTime = System.currentTimeMillis()
            )
            db.paymentDao().insertPayment(payment)

            // 2. Update Customer due & paid balance
            val effectiveDueReduction = amount.coerceAtMost(customer.totalDue)
            db.customerDao().updateCustomerBalance(
                customerId = customer.id,
                additionalPurchase = 0.0,
                additionalPaid = amount,
                additionalDue = -effectiveDueReduction
            )

            // 3. Audit log
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    userId = userId,
                    userName = userName,
                    businessId = businessId,
                    branchId = branchId,
                    action = AuditAction.PAYMENT_RECORD.name,
                    recordId = payment.id,
                    details = "বকেয়া আদায়: ${customer.name} থেকে ৳${amount.toInt()} টাকা গ্রহণ করা হয়েছে"
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
