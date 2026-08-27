package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entity.BranchEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.SaleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ReportPeriod(val banglaTitle: String) {
    TODAY("আজ"),
    THIS_WEEK("এই সপ্তাহ"),
    THIS_MONTH("এই মাস"),
    THIS_YEAR("এই বছর"),
    ALL_TIME("সর্বমোট")
}

data class BranchSalesReport(
    val branch: BranchEntity,
    val totalSales: Double,
    val totalPaid: Double,
    val totalDue: Double,
    val salesCount: Int
)

data class ReportSummary(
    val totalSalesAmount: Double,
    val totalPaidAmount: Double,
    val totalDueAmount: Double,
    val totalExpensesAmount: Double,
    val estimatedGrossProfit: Double,
    val estimatedNetProfit: Double,
    val salesCount: Int
)

class ReportViewModel(private val db: AppDatabase) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.THIS_MONTH)
    val selectedPeriod: StateFlow<ReportPeriod> = _selectedPeriod.asStateFlow()

    fun setPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    private fun getStartTimeForPeriod(period: ReportPeriod): Long {
        val cal = Calendar.getInstance()
        when (period) {
            ReportPeriod.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
            ReportPeriod.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
            }
            ReportPeriod.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
            }
            ReportPeriod.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
            }
            ReportPeriod.ALL_TIME -> return 0L
        }
        return cal.timeInMillis
    }

    fun getReportSummary(businessId: String, branchId: String): Flow<ReportSummary> {
        val salesFlow = db.saleDao().getSalesByBranch(businessId, branchId)
        val expensesFlow = db.expenseDao().getExpensesByBranch(businessId, branchId)

        return combine(salesFlow, expensesFlow, _selectedPeriod) { sales, expenses, period ->
            val startTime = getStartTimeForPeriod(period)
            val filteredSales = sales.filter { it.createdAt >= startTime }
            val filteredExpenses = expenses.filter { it.date >= startTime }

            val totalSales = filteredSales.sumOf { it.total }
            val totalPaid = filteredSales.sumOf { it.paid }
            val totalDue = filteredSales.sumOf { it.due }
            val totalExp = filteredExpenses.sumOf { it.amount }

            // Estimated 20% gross margin on revenue
            val grossProfit = totalSales * 0.20
            val netProfit = (grossProfit - totalExp).coerceAtLeast(0.0)

            ReportSummary(
                totalSalesAmount = totalSales,
                totalPaidAmount = totalPaid,
                totalDueAmount = totalDue,
                totalExpensesAmount = totalExp,
                estimatedGrossProfit = grossProfit,
                estimatedNetProfit = netProfit,
                salesCount = filteredSales.size
            )
        }
    }

    fun getBranchComparison(businessId: String): Flow<List<BranchSalesReport>> {
        val branchesFlow = db.branchDao().getActiveBranches(businessId)
        val allSalesFlow = db.saleDao().getSalesByBusiness(businessId)

        return combine(branchesFlow, allSalesFlow, _selectedPeriod) { branches, sales, period ->
            val startTime = getStartTimeForPeriod(period)
            val filteredSales = sales.filter { it.createdAt >= startTime }

            branches.map { branch ->
                val branchSales = filteredSales.filter { it.branchId == branch.id }
                BranchSalesReport(
                    branch = branch,
                    totalSales = branchSales.sumOf { it.total },
                    totalPaid = branchSales.sumOf { it.paid },
                    totalDue = branchSales.sumOf { it.due },
                    salesCount = branchSales.size
                )
            }
        }
    }

    fun generateCsvReport(businessId: String, branchId: String, summary: ReportSummary, period: ReportPeriod): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateStr = sdf.format(Date())

        return buildString {
            append("আমার দোকান (Amar Dokan) - ব্যবসায়িক রিপোর্ট\n")
            append("রিপোর্ট তৈরির সময়,$dateStr\n")
            append("সময়কাল,${period.banglaTitle}\n\n")
            append("বিবরণ,পরিমাণ (টাকা)\n")
            append("মোট বিক্রি,${summary.totalSalesAmount.toInt()}\n")
            append("মোট নগদ আদায়,${summary.totalPaidAmount.toInt()}\n")
            append("মোট বকেয়া,${summary.totalDueAmount.toInt()}\n")
            append("মোট খরচ,${summary.totalExpensesAmount.toInt()}\n")
            append("আনুমানিক গ্রস লাভ,${summary.estimatedGrossProfit.toInt()}\n")
            append("আনুমানিক নীট লাভ,${summary.estimatedNetProfit.toInt()}\n")
            append("মোট বিক্রয় মেমো সংখ্যা,${summary.salesCount}\n")
        }
    }
}
