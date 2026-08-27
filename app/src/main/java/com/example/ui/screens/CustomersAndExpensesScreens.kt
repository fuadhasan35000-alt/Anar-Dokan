package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.CustomerEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.ShopPermission
import com.example.service.AuthorizationService
import com.example.ui.components.AddExpenseDialog
import com.example.ui.components.RecordPaymentDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.CustomerViewModel
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val customerViewModel = remember { CustomerViewModel(mainViewModel.db) }

    val currentBusiness by mainViewModel.currentBusiness.collectAsState()
    val currentBranch by mainViewModel.currentBranch.collectAsState()
    val currentUser by mainViewModel.currentUser.collectAsState()

    val businessId = currentBusiness?.id ?: ""
    val branchId = currentBranch?.id ?: ""

    val customers by remember(businessId) {
        if (businessId.isNotEmpty()) {
            customerViewModel.getCustomers(businessId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    val searchQuery by customerViewModel.searchQuery.collectAsState()
    val filterDueOnly by customerViewModel.filterDueOnly.collectAsState()

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var customerToPay by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerToEdit by remember { mutableStateOf<CustomerEntity?>(null) }

    val canEditCustomers = AuthorizationService.hasPermission(currentUser, ShopPermission.EDIT_CUSTOMERS)
    val canRecordPayment = AuthorizationService.hasPermission(currentUser, ShopPermission.RECORD_PAYMENT)
    val canSendSms = AuthorizationService.hasPermission(currentUser, ShopPermission.SEND_SMS)
    val canSendWhatsApp = AuthorizationService.hasPermission(currentUser, ShopPermission.SEND_WHATSAPP)

    val totalDueAll = remember(customers) { customers.sumOf { it.totalDue } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (canEditCustomers) {
                ExtendedFloatingActionButton(
                    onClick = { showAddCustomerDialog = true },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("নতুন কাস্টমার", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_customer_fab")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search and Filters
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { customerViewModel.setSearchQuery(it) },
                        placeholder = { Text("কাস্টমারের নাম বা মোবাইল দিয়ে খুঁজুন...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { customerViewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_search_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = filterDueOnly,
                            onClick = { customerViewModel.toggleDueOnly() },
                            label = { Text("বকেয়া কাস্টমার (${customers.count { it.totalDue > 0 }})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DueCardRed,
                                selectedLabelColor = DueTextRed
                            )
                        )

                        Text(
                            text = "মোট বকেয়া: ৳${totalDueAll.toInt()}",
                            fontWeight = FontWeight.Bold,
                            color = DueTextRed,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Customer List
            if (customers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.People, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("কোনো কাস্টমার পাওয়া যায়নি", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (canEditCustomers && searchQuery.isEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { showAddCustomerDialog = true }) {
                                Text("কাস্টমার যুক্ত করুন")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(customers, key = { it.id }) { customer ->
                        val hasDue = customer.totalDue > 0

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (hasDue) DueCardRed.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = customer.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "মোবাইল: ${customer.phone}" + if (customer.address.isNotEmpty()) " • ${customer.address}" else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (hasDue) {
                                        Surface(
                                            color = DueTextRed,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "বাকি: ৳${customer.totalDue.toInt()}",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("মোট ক্রয়: ৳${customer.totalPurchase.toInt()}", style = MaterialTheme.typography.bodySmall)
                                        Text("মোট পরিশোধ: ৳${customer.totalPaid.toInt()}", style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
                                    }

                                    // Action buttons (Pay, SMS, WhatsApp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (hasDue && canRecordPayment) {
                                            Button(
                                                onClick = { customerToPay = customer },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Text("পেমেন্ট নিন", fontSize = 11.sp)
                                            }
                                        }

                                        if (hasDue && canSendSms && customer.phone.isNotEmpty()) {
                                            IconButton(
                                                onClick = {
                                                    mainViewModel.smsService.sendDueReminderSms(
                                                        context,
                                                        customer.name,
                                                        customer.phone,
                                                        customer.totalDue,
                                                        currentBusiness?.name ?: "আমার দোকান"
                                                    )
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(Icons.Default.Sms, contentDescription = "SMS", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            }
                                        }

                                        if (hasDue && canSendWhatsApp && customer.phone.isNotEmpty()) {
                                            IconButton(
                                                onClick = {
                                                    mainViewModel.whatsAppService.sendDueReminder(
                                                        context,
                                                        customer.name,
                                                        customer.phone,
                                                        customer.totalDue,
                                                        currentBusiness?.name ?: "আমার দোকান"
                                                    )
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                                            }
                                        }

                                        if (canEditCustomers) {
                                            IconButton(
                                                onClick = { customerToEdit = customer },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Customer Dialog
    if (showAddCustomerDialog || customerToEdit != null) {
        val editingCustomer = customerToEdit
        var name by remember { mutableStateOf(editingCustomer?.name ?: "") }
        var phone by remember { mutableStateOf(editingCustomer?.phone ?: "") }
        var address by remember { mutableStateOf(editingCustomer?.address ?: "") }
        var initialDue by remember { mutableStateOf(editingCustomer?.totalDue?.toInt()?.toString() ?: "0") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                showAddCustomerDialog = false
                customerToEdit = null
            },
            title = { Text(if (editingCustomer == null) "নতুন কাস্টমার যোগ" else "কাস্টমার সম্পাদনা", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("কাস্টমারের নাম *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_name_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("মোবাইল নম্বর *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_phone_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("ঠিকানা") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (editingCustomer == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = initialDue,
                            onValueChange = { initialDue = it },
                            label = { Text("পূর্বের বকেয়া (যদি থাকে)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank() || phone.isBlank()) {
                            errorMsg = "নাম ও মোবাইল নম্বর আবশ্যক"
                            return@Button
                        }
                        val user = currentUser ?: return@Button
                        val due = initialDue.toDoubleOrNull() ?: 0.0

                        val cust = CustomerEntity(
                            id = editingCustomer?.id ?: UUID.randomUUID().toString(),
                            businessId = businessId,
                            branchId = branchId,
                            name = name.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            totalPurchase = editingCustomer?.totalPurchase ?: due,
                            totalPaid = editingCustomer?.totalPaid ?: 0.0,
                            totalDue = if (editingCustomer == null) due else editingCustomer.totalDue
                        )

                        coroutineScope.launch {
                            customerViewModel.saveCustomer(cust, user.id, user.name)
                            showAddCustomerDialog = false
                            customerToEdit = null
                            mainViewModel.showSnackbar("কাস্টমার তথ্য সংরক্ষিত হয়েছে")
                        }
                    },
                    modifier = Modifier.testTag("save_customer_button")
                ) {
                    Text("সেভ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddCustomerDialog = false
                    customerToEdit = null
                }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Payment Collection Dialog
    if (customerToPay != null) {
        val cust = customerToPay!!
        val user = currentUser
        if (user != null) {
            RecordPaymentDialog(
                customer = cust,
                onRecord = { amount, method, note ->
                    coroutineScope.launch {
                        val res = customerViewModel.recordDuePayment(
                            customer = cust,
                            amount = amount,
                            paymentMethod = method,
                            note = note,
                            userId = user.id,
                            userName = user.name,
                            businessId = businessId,
                            branchId = branchId
                        )
                        customerToPay = null
                        res.fold(
                            onSuccess = { mainViewModel.showSnackbar("৳${amount.toInt()} টাকা বকেয়া আদায় সম্পন্ন হয়েছে") },
                            onFailure = { err -> mainViewModel.showSnackbar(err.localizedMessage ?: "পেমেন্ট ব্যর্থ হয়েছে") }
                        )
                    }
                },
                onDismiss = { customerToPay = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val expenseViewModel = remember { ExpenseViewModel(mainViewModel.db) }

    val currentBusiness by mainViewModel.currentBusiness.collectAsState()
    val currentBranch by mainViewModel.currentBranch.collectAsState()
    val currentUser by mainViewModel.currentUser.collectAsState()

    val businessId = currentBusiness?.id ?: ""
    val branchId = currentBranch?.id ?: ""

    val expenses by remember(businessId, branchId) {
        if (businessId.isNotEmpty() && branchId.isNotEmpty()) {
            expenseViewModel.getExpenses(businessId, branchId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    val selectedCategory by expenseViewModel.selectedCategory.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // Summary
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfToday = cal.timeInMillis

    cal.set(Calendar.DAY_OF_MONTH, 1)
    val startOfMonth = cal.timeInMillis

    val todayTotal = remember(expenses) { expenses.filter { it.date >= startOfToday }.sumOf { it.amount } }
    val monthTotal = remember(expenses) { expenses.filter { it.date >= startOfMonth }.sumOf { it.amount } }

    val canManageExpenses = AuthorizationService.hasPermission(currentUser, ShopPermission.MANAGE_EXPENSES)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (canManageExpenses) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("নতুন খরচ", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_expense_fab")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Stats Snapshot
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("আজকের খরচ", style = MaterialTheme.typography.labelSmall)
                                Text("৳${todayTotal.toInt()}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("চলতি মাসের মোট খরচ", style = MaterialTheme.typography.labelSmall)
                                Text("৳${monthTotal.toInt()}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category filters
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { expenseViewModel.setSelectedCategory(null) },
                                label = { Text("সকল খরচ") }
                            )
                        }
                        items(ExpenseCategory.values()) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat.banglaTitle,
                                onClick = { expenseViewModel.setSelectedCategory(if (selectedCategory == cat.banglaTitle) null else cat.banglaTitle) },
                                label = { Text(cat.banglaTitle) }
                            )
                        }
                    }
                }
            }

            // Expense List
            if (expenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("কোনো খরচ পাওয়া যায়নি", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (canManageExpenses) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { showAddDialog = true }) {
                                Text("খরচ যুক্ত করুন")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(expenses, key = { it.id }) { exp ->
                        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(exp.date))

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(exp.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        text = "${exp.category} • $dateStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (exp.note.isNotEmpty()) {
                                        Text(
                                            text = exp.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "৳${exp.amount.toInt()}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = ErrorRed
                                    )

                                    if (canManageExpenses) {
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    expenseViewModel.deleteExpense(exp.id)
                                                    mainViewModel.showSnackbar("খরচ মুছে ফেলা হয়েছে")
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val user = currentUser
        if (user != null) {
            AddExpenseDialog(
                businessId = businessId,
                branchId = branchId,
                userName = user.name,
                onSave = { expense ->
                    coroutineScope.launch {
                        expenseViewModel.addExpense(expense, user.id, user.name)
                        showAddDialog = false
                        mainViewModel.showSnackbar("খরচ যোগ করা হয়েছে: ৳${expense.amount.toInt()}")
                    }
                },
                onDismiss = { showAddDialog = false }
            )
        }
    }
}
