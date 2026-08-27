package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.BranchEntity
import com.example.data.entity.BusinessEntity
import com.example.data.entity.CustomerEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.UserEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMethod
import com.example.data.model.ShopPermission
import com.example.data.model.StockChangeType
import com.example.data.model.UserRole
import java.util.UUID

@Composable
fun BusinessSelectorDialog(
    businesses: List<BusinessEntity>,
    currentBusinessId: String?,
    onSelectBusiness: (BusinessEntity) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("ব্যবসা নির্বাচন করুন", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(businesses) { b ->
                    val isSelected = b.id == currentBusinessId
                    ListItem(
                        headlineContent = {
                            Text(
                                b.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        supportingContent = { Text("মালিক: ${b.ownerName} • ${b.phone}") },
                        leadingContent = {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Store,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSelectBusiness(b)
                                onDismiss()
                            }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন")
            }
        }
    )
}

@Composable
fun BranchSelectorDialog(
    branches: List<BranchEntity>,
    currentBranchId: String?,
    onSelectBranch: (BranchEntity) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("শাখা পরিবর্তন করুন", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(branches) { branch ->
                    val isSelected = branch.id == currentBranchId
                    ListItem(
                        headlineContent = {
                            Text(
                                branch.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        supportingContent = { Text(branch.address) },
                        leadingContent = {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSelectBranch(branch)
                                onDismiss()
                            }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন")
            }
        }
    )
}

@Composable
fun AddEditProductDialog(
    initialProduct: ProductEntity? = null,
    businessId: String,
    branchId: String,
    onSave: (ProductEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var sku by remember { mutableStateOf(initialProduct?.sku ?: "") }
    var barcode by remember { mutableStateOf(initialProduct?.barcode ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: "সাধারণ") }
    var purchasePrice by remember { mutableStateOf(initialProduct?.purchasePrice?.toString() ?: "") }
    var salePrice by remember { mutableStateOf(initialProduct?.salePrice?.toString() ?: "") }
    var currentStock by remember { mutableStateOf(initialProduct?.currentStock?.toString() ?: "10") }
    var minimumStock by remember { mutableStateOf(initialProduct?.minimumStock?.toString() ?: "5") }
    var unit by remember { mutableStateOf(initialProduct?.unit ?: "পিস") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (initialProduct == null) "নতুন পণ্য যোগ করুন" else "পণ্য সম্পাদনা",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("পণ্যের নাম *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("ক্যাটাগরি") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("একক (পিস/কেজি)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = purchasePrice,
                        onValueChange = { purchasePrice = it },
                        label = { Text("ক্রয় মূল্য (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = salePrice,
                        onValueChange = { salePrice = it },
                        label = { Text("বিক্রয় মূল্য (৳) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentStock,
                        onValueChange = { currentStock = it },
                        label = { Text("বর্তমান স্টক *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minimumStock,
                        onValueChange = { minimumStock = it },
                        label = { Text("কম স্টক সীমা") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU কোড") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("বারকোড") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("বাতিল")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "দয়া করে পণ্যের নাম লিখুন"
                                return@Button
                            }
                            val saleP = salePrice.toDoubleOrNull() ?: 0.0
                            if (saleP <= 0) {
                                errorMessage = "সঠিক বিক্রয় মূল্য দিন"
                                return@Button
                            }
                            val purchP = purchasePrice.toDoubleOrNull() ?: 0.0
                            val currStock = currentStock.toDoubleOrNull() ?: 0.0
                            val minStock = minimumStock.toDoubleOrNull() ?: 5.0

                            val entity = ProductEntity(
                                id = initialProduct?.id ?: UUID.randomUUID().toString(),
                                businessId = businessId,
                                branchId = branchId,
                                name = name.trim(),
                                sku = sku.trim(),
                                barcode = barcode.trim(),
                                category = category.trim().ifEmpty { "সাধারণ" },
                                purchasePrice = purchP,
                                salePrice = saleP,
                                currentStock = currStock,
                                minimumStock = minStock,
                                unit = unit.trim().ifEmpty { "পিস" }
                            )
                            onSave(entity)
                        },
                        modifier = Modifier.testTag("save_product_button")
                    ) {
                        Text("সেভ করুন")
                    }
                }
            }
        }
    }
}

@Composable
fun StockAdjustDialog(
    product: ProductEntity,
    onAdjust: (type: StockChangeType, qty: Double, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(StockChangeType.STOCK_IN) }
    var quantityStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("স্টক পরিবর্তন: ${product.name}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "বর্তমান স্টক: ${product.currentStock.toInt()} ${product.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(StockChangeType.STOCK_IN, StockChangeType.STOCK_OUT, StockChangeType.ADJUSTMENT).forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.banglaTitle, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text(if (selectedType == StockChangeType.ADJUSTMENT) "নতুন নির্ধারিত স্টক" else "পরিমাণ (${product.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stock_qty_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("মন্তব্য / কারণ") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityStr.toDoubleOrNull()
                    if (qty == null || (selectedType != StockChangeType.ADJUSTMENT && qty <= 0)) {
                        errorMessage = "সঠিক পরিমাণ দিন"
                        return@Button
                    }
                    onAdjust(selectedType, qty, note)
                },
                modifier = Modifier.testTag("confirm_stock_adjust")
            ) {
                Text("আপডেট করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
fun RecordPaymentDialog(
    customer: CustomerEntity,
    onRecord: (amount: Double, method: String, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountStr by remember { mutableStateOf(customer.totalDue.toInt().toString()) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH.banglaTitle) }
    var note by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("বকেয়া আদায় / পেমেন্ট গ্রহণ", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("কাস্টমার: ${customer.name}", fontWeight = FontWeight.Medium)
                Text("বর্তমান মোট বকেয়া: ৳${customer.totalDue.toInt()}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("আদায়কৃত টাকা (৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("পেমেন্ট মেথড:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("নগদ (Cash)", "বিকাশ (bKash)", "নগদ (Nagad)").forEach { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(method, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("মন্তব্য (ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull()
                    if (amt == null || amt <= 0) {
                        errorMessage = "সঠিক টাকার পরিমাণ দিন"
                        return@Button
                    }
                    onRecord(amt, selectedMethod, note)
                },
                modifier = Modifier.testTag("confirm_payment_record")
            ) {
                Text("আদায় সম্পন্ন করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
fun AddExpenseDialog(
    businessId: String,
    branchId: String,
    userName: String,
    onSave: (ExpenseEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.SHOP_RENT.banglaTitle) }
    var note by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("নতুন খরচ যুক্ত করুন", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("খরচের শিরোনাম *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("টাকার পরিমাণ (৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("ক্যাটাগরি:", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ExpenseCategory.values().toList().chunked(2).forEach { rowList ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            rowList.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat.banglaTitle,
                                    onClick = { selectedCategory = cat.banglaTitle },
                                    label = { Text(cat.banglaTitle, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("বিস্তারিত নোট") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = "খরচের শিরোনাম দিন"
                        return@Button
                    }
                    val amt = amountStr.toDoubleOrNull()
                    if (amt == null || amt <= 0) {
                        errorMessage = "সঠিক টাকার পরিমাণ দিন"
                        return@Button
                    }

                    val expense = ExpenseEntity(
                        businessId = businessId,
                        branchId = branchId,
                        title = title.trim(),
                        category = selectedCategory,
                        amount = amt,
                        note = note.trim(),
                        createdBy = userName
                    )
                    onSave(expense)
                },
                modifier = Modifier.testTag("save_expense_button")
            ) {
                Text("খরচ সেভ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
