package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.data.database.AppDatabase
import com.example.data.entity.AuditLogEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.model.AuditAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class ExpenseViewModel(private val db: AppDatabase) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    fun setSelectedCategory(cat: String?) {
        _selectedCategory.value = cat
    }

    fun getExpenses(businessId: String, branchId: String): Flow<List<ExpenseEntity>> {
        return db.expenseDao().getExpensesByBranch(businessId, branchId).combine(_selectedCategory) { list, cat ->
            if (cat == null) list else list.filter { it.category == cat }
        }
    }

    suspend fun addExpense(
        expense: ExpenseEntity,
        userId: String,
        userName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (expense.amount <= 0) {
            return@withContext Result.failure(Exception("খরচের পরিমাণ ০ এর বেশি হতে হবে!"))
        }

        try {
            db.expenseDao().insertExpense(expense)
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    userId = userId,
                    userName = userName,
                    businessId = expense.businessId,
                    branchId = expense.branchId,
                    action = AuditAction.EXPENSE_ADD.name,
                    recordId = expense.id,
                    details = "খরচ যুক্ত: ${expense.title} (৳${expense.amount.toInt()} - ${expense.category})"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(id: String) = withContext(Dispatchers.IO) {
        db.expenseDao().deleteExpense(id)
    }
}
