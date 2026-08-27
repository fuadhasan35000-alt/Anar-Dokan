package com.example.service

import com.example.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class BusinessSummary(
    val businessName: String,
    val branchName: String,
    val todaySalesTotal: Double,
    val todaySalesCount: Int,
    val todayReceivedTotal: Double,
    val totalDueAmount: Double,
    val topDueCustomer: String,
    val topDueAmount: Double,
    val lowStockCount: Int,
    val lowStockProductNames: List<String>,
    val todayExpenseTotal: Double,
    val monthlyExpenseTotal: Double,
    val estimatedProfit: Double,
    val totalProductsCount: Int,
    val totalCustomersCount: Int
)

class AiDataSummaryService(private val db: AppDatabase) {

    suspend fun getBusinessSummary(businessId: String, branchId: String): BusinessSummary = withContext(Dispatchers.IO) {
        val business = db.businessDao().getBusinessById(businessId)
        val branch = db.branchDao().getBranchById(branchId)

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, 1)
        val startOfMonth = cal.timeInMillis

        // Sales today
        val allSales = db.saleDao().getSalesByBranch(businessId, branchId).firstOrNull() ?: emptyList()
        val todaySales = allSales.filter { it.createdAt >= startOfToday }
        val todaySalesTotal = todaySales.sumOf { it.total }
        val todayReceivedTotal = todaySales.sumOf { it.paid }

        // Products & Stock
        val products = db.productDao().getProductsByBranch(businessId, branchId).firstOrNull() ?: emptyList()
        val lowStockProducts = products.filter { it.currentStock <= it.minimumStock }

        // Customers & Dues
        val customers = db.customerDao().getCustomersByBusiness(businessId).firstOrNull() ?: emptyList()
        val totalDue = customers.sumOf { it.totalDue }
        val topDueCust = customers.maxByOrNull { it.totalDue }

        // Expenses
        val expenses = db.expenseDao().getExpensesByBranch(businessId, branchId).firstOrNull() ?: emptyList()
        val todayExpenses = expenses.filter { it.date >= startOfToday }.sumOf { it.amount }
        val monthlyExpenses = expenses.filter { it.date >= startOfMonth }.sumOf { it.amount }

        // Estimated profit calculation (Approx: today sales - expenses - estimated 15-20% margin or cost)
        val estimatedMargin = todaySales.sumOf { sale ->
            sale.total * 0.20 // fallback estimated gross margin
        }
        val estimatedProfit = (estimatedMargin - todayExpenses).coerceAtLeast(0.0)

        BusinessSummary(
            businessName = business?.name ?: "দোকান",
            branchName = branch?.name ?: "প্রধান শাখা",
            todaySalesTotal = todaySalesTotal,
            todaySalesCount = todaySales.size,
            todayReceivedTotal = todayReceivedTotal,
            totalDueAmount = totalDue,
            topDueCustomer = topDueCust?.name ?: "কেউ নেই",
            topDueAmount = topDueCust?.totalDue ?: 0.0,
            lowStockCount = lowStockProducts.size,
            lowStockProductNames = lowStockProducts.take(5).map { "${it.name} (${it.currentStock.toInt()} ${it.unit})" },
            todayExpenseTotal = todayExpenses,
            monthlyExpenseTotal = monthlyExpenses,
            estimatedProfit = estimatedProfit,
            totalProductsCount = products.size,
            totalCustomersCount = customers.size
        )
    }
}

class AiAssistantService(
    private val summaryService: AiDataSummaryService
) {
    suspend fun answerQuestion(
        businessId: String,
        branchId: String,
        question: String
    ): String = withContext(Dispatchers.IO) {
        val summary = summaryService.getBusinessSummary(businessId, branchId)
        val q = question.trim().lowercase()

        when {
            q.contains("আজ") && (q.contains("বিক্রি") || q.contains("সেল")) -> {
                "আজ আপনার '${summary.businessName}' দোকানে মোট ${summary.todaySalesCount} টি বিক্রিতে ৳${summary.todaySalesTotal.toInt()} টাকা বিক্রি হয়েছে। এর মধ্যে নগদ আদায় হয়েছে ৳${summary.todayReceivedTotal.toInt()} টাকা।"
            }
            q.contains("বাকি") || q.contains("বকেয়া") || q.contains("দেনা") -> {
                if (summary.topDueAmount > 0) {
                    "দোকানের মোট বকেয়ার পরিমাণ ৳${summary.totalDueAmount.toInt()} টাকা।\nসবচেয়ে বেশি বাকি আছে '${summary.topDueCustomer}' এর কাছে, যার বকেয়া ৳${summary.topDueAmount.toInt()} টাকা।"
                } else {
                    "আলহামদুলিল্লাহ! আপনার দোকানে বর্তমানে কোনো বকেয়া বাকি নেই।"
                }
            }
            q.contains("কমে") || q.contains("স্টক") || q.contains("কম") || q.contains("ফুরিয়ে") -> {
                if (summary.lowStockCount > 0) {
                    val list = summary.lowStockProductNames.joinToString(", ")
                    "বর্তমানে ${summary.lowStockCount} টি পণ্যের স্টক নির্ধারিত সীমার নিচে রয়েছে:\n$list\n\nদ্রুত নতুন স্টক তোলার পরামর্শ দেওয়া হচ্ছে।"
                } else {
                    "সব পণ্যের পর্যাপ্ত স্টক মজুত রয়েছে। কোনো পণ্যের সংকট নেই।"
                }
            }
            q.contains("খরচ") || q.contains("ব্যয়") -> {
                "আজকের মোট খরচ ৳${summary.todayExpenseTotal.toInt()} টাকা এবং চলতি মাসের সর্বমোট খরচ ৳${summary.monthlyExpenseTotal.toInt()} টাকা।"
            }
            q.contains("লাভ") || q.contains("প্রফিট") -> {
                "আজকের আনুমানিক নীট লাভ প্রায় ৳${summary.estimatedProfit.toInt()} টাকা (আজকের বিক্রি ও খরচের ভিত্তিতে হিসাবকৃত)।"
            }
            q.contains("পণ্য") || q.contains("আইটেম") -> {
                "দোকানে মোট ${summary.totalProductsCount} টি রেজিস্টার্ড পণ্য রয়েছে, যার মধ্যে ${summary.lowStockCount} টি আইটেম স্টক অ্যালার্টে আছে।"
            }
            q.contains("কাস্টমার") || q.contains("গ্রাহক") -> {
                "আপনার সিস্টেমে মোট ${summary.totalCustomersCount} জন নিয়মিত কাস্টমার তালিকাভুক্ত আছেন।"
            }
            else -> {
                "আমি আপনার 'দোকান AI' সহকারী। আপনি আমাকে আজকের বিক্রি, বকেয়া হিসাব, কম স্টক, খরচ বা আনুমানিক লাভ সংক্রান্ত যেকোনো প্রশ্ন করতে পারেন।"
            }
        }
    }
}
