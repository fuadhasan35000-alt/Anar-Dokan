package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CustomerEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.SaleEntity
import com.example.data.model.ShopPermission
import com.example.service.AuthorizationService
import com.example.ui.components.AddEditProductDialog
import com.example.ui.components.AddExpenseDialog
import com.example.ui.components.StockAdjustDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val currentBusiness by viewModel.currentBusiness.collectAsState()
    val currentBranch by viewModel.currentBranch.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val businessId = currentBusiness?.id ?: ""
    val branchId = currentBranch?.id ?: ""

    // Live data collection
    val products by remember(businessId, branchId) {
        if (businessId.isNotEmpty() && branchId.isNotEmpty()) {
            viewModel.db.productDao().getProductsByBranch(businessId, branchId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    val sales by remember(businessId, branchId) {
        if (businessId.isNotEmpty() && branchId.isNotEmpty()) {
            viewModel.db.saleDao().getSalesByBranch(businessId, branchId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    val customers by remember(businessId) {
        if (businessId.isNotEmpty()) {
            viewModel.db.customerDao().getCustomersByBusiness(businessId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    val expenses by remember(businessId, branchId) {
        if (businessId.isNotEmpty() && branchId.isNotEmpty()) {
            viewModel.db.expenseDao().getExpensesByBranch(businessId, branchId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    // Calculations
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfToday = cal.timeInMillis

    val todaySalesList = remember(sales, startOfToday) { sales.filter { it.createdAt >= startOfToday } }
    val todaySalesTotal = remember(todaySalesList) { todaySalesList.sumOf { it.total } }
    val todayPaidTotal = remember(todaySalesList) { todaySalesList.sumOf { it.paid } }
    val totalDueAmount = remember(customers) { customers.sumOf { it.totalDue } }
    val lowStockProducts = remember(products) { products.filter { it.currentStock <= it.minimumStock } }
    val todayExpensesTotal = remember(expenses, startOfToday) {
        expenses.filter { it.date >= startOfToday }.sumOf { it.amount }
    }
    val estimatedProfit = remember(todaySalesTotal, todayExpensesTotal) {
        ((todaySalesTotal * 0.20) - todayExpensesTotal).coerceAtLeast(0.0)
    }

    // Dialog States
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var selectedStockProduct by remember { mutableStateOf<ProductEntity?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Welcome & Today Snapshot
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "আজকের সারসংক্ষেপ",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            )
                            Text(
                                text = "৳${todaySalesTotal.toInt()}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("নগদ আদায়", style = MaterialTheme.typography.labelSmall)
                            Text("৳${todayPaidTotal.toInt()}", fontWeight = FontWeight.Bold, color = SuccessGreen, fontSize = 16.sp)
                        }
                        Column {
                            Text("আজকের খরচ", style = MaterialTheme.typography.labelSmall)
                            Text("৳${todayExpensesTotal.toInt()}", fontWeight = FontWeight.Bold, color = ErrorRed, fontSize = 16.sp)
                        }
                        Column {
                            Text("আনুমানিক লাভ", style = MaterialTheme.typography.labelSmall)
                            Text("৳${estimatedProfit.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        // 2. Low Stock Alert Banner (if applicable)
        if (lowStockProducts.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DueCardRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = DueTextRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${lowStockProducts.size} টি পণ্যের স্টক ফুরিয়ে আসছে!",
                                fontWeight = FontWeight.Bold,
                                color = DueTextRed,
                                fontSize = 14.sp
                            )
                            Text(
                                text = lowStockProducts.take(2).joinToString(", ") { it.name } + if (lowStockProducts.size > 2) " ইত্যাদি" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = DueTextRed.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick = { viewModel.navigateTo(AppScreen.PRODUCTS) }) {
                            Text("স্টক দেখুন", color = DueTextRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Quick Action Grid
        item {
            Text(
                text = "দ্রুত কার্যক্রম",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickActionButton(
                    icon = Icons.Default.PointOfSale,
                    label = "নতুন বিক্রি",
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.POS) }
                )
                QuickActionButton(
                    icon = Icons.Default.AddBox,
                    label = "+ পণ্য যোগ",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = { showAddProductDialog = true }
                )
                QuickActionButton(
                    icon = Icons.Default.PersonAdd,
                    label = "+ কাস্টমার",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.CUSTOMERS) }
                )
                QuickActionButton(
                    icon = Icons.Default.ReceiptLong,
                    label = "+ খরচ",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    onClick = { showAddExpenseDialog = true }
                )
            }
        }

        // 4. Secondary Stat Counters
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatMiniCard(
                    title = "মোট বকেয়া",
                    value = "৳${totalDueAmount.toInt()}",
                    icon = Icons.Default.MonetizationOn,
                    color = DueTextRed,
                    bgColor = DueCardRed,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.CUSTOMERS) }
                )
                StatMiniCard(
                    title = "মোট পণ্য স্টক",
                    value = "${products.size} টি",
                    icon = Icons.Default.Inventory2,
                    color = EmeraldPrimary,
                    bgColor = EmeraldPrimaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.PRODUCTS) }
                )
                StatMiniCard(
                    title = "কাস্টমার সংখ্যা",
                    value = "${customers.size} জন",
                    icon = Icons.Default.PeopleAlt,
                    color = GoldTertiary,
                    bgColor = GoldTertiaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.CUSTOMERS) }
                )
            }
        }

        // 5. Recent Sales
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "সাম্প্রতিক বিক্রি",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                if (sales.isNotEmpty()) {
                    TextButton(onClick = { viewModel.navigateTo(AppScreen.REPORTS) }) {
                        Text("সব দেখুন")
                    }
                }
            }
        }

        if (sales.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("এখনো কোনো বিক্রি রেকর্ড করা হয়নি।", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.navigateTo(AppScreen.POS) }) {
                            Text("প্রথম বিক্রি করুন")
                        }
                    }
                }
            }
        } else {
            items(sales.take(5)) { sale ->
                val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(sale.createdAt))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.viewInvoice(sale) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sale.customerName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${sale.invoiceNumber} • $dateStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "৳${sale.total.toInt()}",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp
                            )
                            if (sale.due > 0) {
                                Text(
                                    text = "বাকি: ৳${sale.due.toInt()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DueTextRed,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    text = "পরিশোধিত",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SuccessGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddProductDialog) {
        AddEditProductDialog(
            businessId = businessId,
            branchId = branchId,
            onSave = { product ->
                coroutineScope.launch {
                    val user = currentUser ?: return@launch
                    viewModel.db.productDao().insertProduct(product)
                    showAddProductDialog = false
                    viewModel.showSnackbar("পণ্য যুক্ত করা হয়েছে: ${product.name}")
                }
            },
            onDismiss = { showAddProductDialog = false }
        )
    }

    if (showAddExpenseDialog) {
        val user = currentUser
        if (user != null) {
            AddExpenseDialog(
                businessId = businessId,
                branchId = branchId,
                userName = user.name,
                onSave = { expense ->
                    coroutineScope.launch {
                        viewModel.db.expenseDao().insertExpense(expense)
                        showAddExpenseDialog = false
                        viewModel.showSnackbar("খরচ যোগ করা হয়েছে: ৳${expense.amount.toInt()}")
                    }
                },
                onDismiss = { showAddExpenseDialog = false }
            )
        }
    }

    if (selectedStockProduct != null) {
        val product = selectedStockProduct!!
        val user = currentUser
        if (user != null) {
            StockAdjustDialog(
                product = product,
                onAdjust = { type, qty, note ->
                    coroutineScope.launch {
                        val newStock = when (type) {
                            com.example.data.model.StockChangeType.STOCK_IN -> product.currentStock + qty
                            com.example.data.model.StockChangeType.STOCK_OUT -> (product.currentStock - qty).coerceAtLeast(0.0)
                            else -> qty
                        }
                        viewModel.db.productDao().updateStock(product.id, newStock)
                        selectedStockProduct = null
                        viewModel.showSnackbar("স্টক আপডেট সম্পন্ন হয়েছে")
                    }
                },
                onDismiss = { selectedStockProduct = null }
            )
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun StatMiniCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
        }
    }
}
