package com.example.service

import android.content.Context
import android.net.Uri
import com.example.data.database.AppDatabase
import com.example.data.entity.AuditLogEntity
import com.example.data.entity.CustomerEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.SaleEntity
import com.example.data.model.AuditAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRestoreService(
    private val db: AppDatabase,
    private val context: Context
) {

    suspend fun exportBackupJson(businessId: String): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("businessId", businessId)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("app", "আমার দোকান (Amar Dokan)")

        // Products
        val products = db.productDao().getProductsByBusiness(businessId).firstOrNull() ?: emptyList()
        val prodArray = JSONArray()
        products.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("businessId", p.businessId)
                put("branchId", p.branchId)
                put("name", p.name)
                put("sku", p.sku)
                put("barcode", p.barcode)
                put("category", p.category)
                put("purchasePrice", p.purchasePrice)
                put("salePrice", p.salePrice)
                put("currentStock", p.currentStock)
                put("minimumStock", p.minimumStock)
                put("unit", p.unit)
                put("supplier", p.supplier)
            }
            prodArray.put(obj)
        }
        root.put("products", prodArray)

        // Customers
        val customers = db.customerDao().getCustomersByBusiness(businessId).firstOrNull() ?: emptyList()
        val custArray = JSONArray()
        customers.forEach { c ->
            val obj = JSONObject().apply {
                put("id", c.id)
                put("businessId", c.businessId)
                put("branchId", c.branchId)
                put("name", c.name)
                put("phone", c.phone)
                put("address", c.address)
                put("totalPurchase", c.totalPurchase)
                put("totalPaid", c.totalPaid)
                put("totalDue", c.totalDue)
            }
            custArray.put(obj)
        }
        root.put("customers", custArray)

        // Sales
        val sales = db.saleDao().getSalesByBusiness(businessId).firstOrNull() ?: emptyList()
        val salesArray = JSONArray()
        sales.forEach { s ->
            val obj = JSONObject().apply {
                put("id", s.id)
                put("invoiceNumber", s.invoiceNumber)
                put("businessId", s.businessId)
                put("branchId", s.branchId)
                put("customerName", s.customerName)
                put("subtotal", s.subtotal)
                put("discount", s.discount)
                put("total", s.total)
                put("paid", s.paid)
                put("due", s.due)
                put("paymentMethod", s.paymentMethod)
                put("createdAt", s.createdAt)
            }
            salesArray.put(obj)
        }
        root.put("sales", salesArray)

        // Expenses
        val expenses = db.expenseDao().getExpensesByBusiness(businessId).firstOrNull() ?: emptyList()
        val expArray = JSONArray()
        expenses.forEach { e ->
            val obj = JSONObject().apply {
                put("id", e.id)
                put("businessId", e.businessId)
                put("branchId", e.branchId)
                put("title", e.title)
                put("category", e.category)
                put("amount", e.amount)
                put("date", e.date)
                put("note", e.note)
            }
            expArray.put(obj)
        }
        root.put("expenses", expArray)

        root.toString(2)
    }

    suspend fun writeBackupToUri(uri: Uri, businessId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = exportBackupJson(businessId)
            context.contentResolver.openOutputStream(uri)?.use { output ->
                OutputStreamWriter(output).use { writer ->
                    writer.write(json)
                }
            } ?: return@withContext Result.failure(Exception("ফাইল খোলা সম্ভব হয়নি।"))

            db.auditLogDao().insertLog(
                AuditLogEntity(
                    userId = "system",
                    userName = "অ্যাডমিন",
                    businessId = businessId,
                    branchId = "all",
                    action = AuditAction.BACKUP.name,
                    details = "ডাটা ব্যাকআপ এক্সপোর্ট করা হয়েছে।"
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackupFromUri(uri: Uri, targetBusinessId: String, targetBranchId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val stringBuilder = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                }
            } ?: return@withContext Result.failure(Exception("ফাইল পড়তে পারা যায়নি।"))

            val root = JSONObject(stringBuilder.toString())
            var count = 0

            // Restore products
            if (root.has("products")) {
                val prodArray = root.getJSONArray("products")
                for (i in 0 until prodArray.length()) {
                    val p = prodArray.getJSONObject(i)
                    val entity = ProductEntity(
                        id = p.optString("id"),
                        businessId = targetBusinessId,
                        branchId = targetBranchId,
                        name = p.optString("name", "পণ্য"),
                        sku = p.optString("sku", ""),
                        barcode = p.optString("barcode", ""),
                        category = p.optString("category", "সাধারণ"),
                        purchasePrice = p.optDouble("purchasePrice", 0.0),
                        salePrice = p.optDouble("salePrice", 0.0),
                        currentStock = p.optDouble("currentStock", 0.0),
                        minimumStock = p.optDouble("minimumStock", 5.0),
                        unit = p.optString("unit", "পিস"),
                        supplier = p.optString("supplier", "")
                    )
                    db.productDao().insertProduct(entity)
                    count++
                }
            }

            // Restore customers
            if (root.has("customers")) {
                val custArray = root.getJSONArray("customers")
                for (i in 0 until custArray.length()) {
                    val c = custArray.getJSONObject(i)
                    val entity = CustomerEntity(
                        id = c.optString("id"),
                        businessId = targetBusinessId,
                        branchId = targetBranchId,
                        name = c.optString("name", "কাস্টমার"),
                        phone = c.optString("phone", ""),
                        address = c.optString("address", ""),
                        totalPurchase = c.optDouble("totalPurchase", 0.0),
                        totalPaid = c.optDouble("totalPaid", 0.0),
                        totalDue = c.optDouble("totalDue", 0.0)
                    )
                    db.customerDao().insertCustomer(entity)
                    count++
                }
            }

            db.auditLogDao().insertLog(
                AuditLogEntity(
                    userId = "system",
                    userName = "অ্যাডমিন",
                    businessId = targetBusinessId,
                    branchId = targetBranchId,
                    action = AuditAction.RESTORE.name,
                    details = "ব্যাকআপ থেকে $count টি রেকর্ড রিস্টোর করা হয়েছে।"
                )
            )

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
