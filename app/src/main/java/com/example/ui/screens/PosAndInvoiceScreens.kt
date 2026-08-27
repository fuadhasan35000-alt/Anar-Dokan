package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CustomerEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.SaleEntity
import com.example.data.entity.SaleItemEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val posViewModel = remember { PosViewModel(mainViewModel.db) }

    val currentBusiness by mainViewModel.currentBusiness.collectAsState()
    val currentBranch by mainViewModel.currentBranch.collectAsState()
    val currentUser by mainViewModel.currentUser.collectAsState()

    val businessId = currentBusiness?.id ?: ""
    val branchId = currentBranch?.id ?: ""

    // Products & Customers
    val products by remember(businessId, branchId) {
        if (businessId.isNotEmpty() && branchId.isNotEmpty()) {
            mainViewModel.db.productDao().getProductsByBranch(businessId, branchId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    val customers by remember(businessId) {
        if (businessId.isNotEmpty()) {
            mainViewModel.db.customerDao().getCustomersByBusiness(businessId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    // Pos States
    val cartItems by posViewModel.cartItems.collectAsState()
    val selectedCustomer by posViewModel.selectedCustomer.collectAsState()
    val searchQuery by posViewModel.searchQuery.collectAsState()
    val subtotal by posViewModel.subtotal.collectAsState()
    val discount by posViewModel.discount.collectAsState()
    val grandTotal by posViewModel.grandTotal.collectAsState()
    val paidAmount by posViewModel.paidAmount.collectAsState()
    val dueAmount by posViewModel.dueAmount.collectAsState()
    val paymentMethod by posViewModel.paymentMethod.collectAsState()

    var showCustomerPicker by remember { mutableStateOf(false) }
    var isCheckingOut by remember { mutableStateOf(false) }
    var discountInput by remember { mutableStateOf("") }
    var paidInput by remember { mutableStateOf("") }

    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isEmpty()) products else {
            products.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.sku.contains(searchQuery, ignoreCase = true) ||
                        it.barcode.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top product search & quick selector
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { posViewModel.setSearchQuery(it) },
                    placeholder = { Text("পণ্য খুঁজুন বা বারকোড টাইপ করুন...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { posViewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pos_search_input")
                )

                if (filteredProducts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredProducts.take(8)) { prod ->
                            val inStock = prod.currentStock > 0
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (inStock) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = inStock) {
                                        posViewModel.addToCart(prod)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Text(
                                        text = prod.name,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "৳${prod.salePrice.toInt()} (${prod.currentStock.toInt()} ${prod.unit})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (inStock) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Middle: Cart List + Checkout Summary
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Customer Banner
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = selectedCustomer?.name ?: "সাধারণ কাস্টমার (Cash Customer)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (selectedCustomer != null) {
                                    Text(
                                        text = "বর্তমান বাকি: ৳${selectedCustomer?.totalDue?.toInt() ?: 0}",
                                        fontSize = 12.sp,
                                        color = DueTextRed
                                    )
                                }
                            }
                        }

                        TextButton(onClick = { showCustomerPicker = true }) {
                            Text(if (selectedCustomer == null) "কাস্টমার যুক্ত" else "পরিবর্তন")
                        }
                    }
                }
            }

            // Cart Items Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "কার্ট তালিকা (${cartItems.size} টি পণ্য)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (cartItems.isNotEmpty()) {
                        TextButton(onClick = { posViewModel.clearCart() }) {
                            Text("কার্ট খালি করুন", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (cartItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("কার্ট বর্তমানে খালি", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            Text("উপরে পণ্য নির্বাচন করে কার্টে যোগ করুন", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            } else {
                items(cartItems, key = { it.product.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.product.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("দর: ৳${item.unitPrice.toInt()} / ${item.product.unit}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("মোট: ৳${item.subtotal.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                            }

                            // Stepper
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { posViewModel.updateQuantity(item.product.id, item.quantity - 1.0) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease", tint = MaterialTheme.colorScheme.outline)
                                }

                                Text(
                                    text = "${item.quantity.toInt()} ${item.product.unit}",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )

                                IconButton(
                                    onClick = { posViewModel.updateQuantity(item.product.id, item.quantity + 1.0) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase", tint = MaterialTheme.colorScheme.primary)
                                }

                                IconButton(
                                    onClick = { posViewModel.removeFromCart(item.product.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ErrorRed)
                                }
                            }
                        }
                    }
                }

                // Billing Details Card
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("বিল ও পেমেন্ট বিবরণী", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("সাবটোটাল:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("৳${subtotal.toInt()}", fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Discount & Paid Inputs
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = discountInput,
                                    onValueChange = {
                                        discountInput = it
                                        posViewModel.setDiscount(it.toDoubleOrNull() ?: 0.0)
                                    },
                                    label = { Text("ছাড় (৳)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("pos_discount_input")
                                )

                                OutlinedTextField(
                                    value = paidInput,
                                    onValueChange = {
                                        paidInput = it
                                        posViewModel.setPaidAmount(it.toDoubleOrNull())
                                    },
                                    label = { Text("পরিশোধ (৳)") },
                                    placeholder = { Text("৳${grandTotal.toInt()}") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("pos_paid_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Payment Method
                            Text("পেমেন্ট মাধ্যম:", style = MaterialTheme.typography.labelSmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("নগদ (Cash)", "বিকাশ (bKash)", "নগদ (Nagad)").forEach { method ->
                                    FilterChip(
                                        selected = paymentMethod == method,
                                        onClick = { posViewModel.setPaymentMethod(method) },
                                        label = { Text(method, fontSize = 11.sp) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("সর্বমোট প্রদেয়:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("৳${grandTotal.toInt()}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                            }

                            if (dueAmount > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("বর্তমান বকেয়া:", fontWeight = FontWeight.Bold, color = DueTextRed)
                                    Text("৳${dueAmount.toInt()}", fontWeight = FontWeight.ExtraBold, color = DueTextRed, fontSize = 16.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Confirm Sale Button
                            Button(
                                onClick = {
                                    val user = currentUser
                                    if (user == null) {
                                        mainViewModel.showSnackbar("ব্যবহারকারী পাওয়া যায়নি!")
                                        return@Button
                                    }
                                    isCheckingOut = true
                                    coroutineScope.launch {
                                        val result = posViewModel.completeSale(businessId, branchId, user)
                                        isCheckingOut = false
                                        result.fold(
                                            onSuccess = { sale ->
                                                mainViewModel.viewInvoice(sale)
                                                mainViewModel.showSnackbar("বিক্রি সফলভাবে সম্পন্ন হয়েছে!")
                                            },
                                            onFailure = { err ->
                                                mainViewModel.showSnackbar(err.localizedMessage ?: "বিক্রি সম্পন্ন করা যায়নি")
                                            }
                                        )
                                    }
                                },
                                enabled = !isCheckingOut,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("pos_complete_sale_button")
                            ) {
                                if (isCheckingOut) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("বিক্রি সম্পন্ন করুন (৳${grandTotal.toInt()})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Customer Picker Dialog
    if (showCustomerPicker) {
        AlertDialog(
            onDismissRequest = { showCustomerPicker = false },
            title = { Text("কাস্টমার নির্বাচন করুন", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        ListItem(
                            headlineContent = { Text("সাধারণ কাস্টমার (Cash Customer)", fontWeight = FontWeight.Bold) },
                            leadingContent = { Icon(Icons.Default.PersonOutline, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    posViewModel.selectCustomer(null)
                                    showCustomerPicker = false
                                }
                        )
                        HorizontalDivider()
                    }

                    items(customers) { cust ->
                        ListItem(
                            headlineContent = { Text(cust.name, fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("মোবাইল: ${cust.phone} • বকেয়া: ৳${cust.totalDue.toInt()}") },
                            leadingContent = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    posViewModel.selectCustomer(cust)
                                    showCustomerPicker = false
                                }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomerPicker = false }) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }
}

@Composable
fun InvoiceScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentBusiness by viewModel.currentBusiness.collectAsState()
    val currentBranch by viewModel.currentBranch.collectAsState()
    val sale by viewModel.activeInvoice.collectAsState()
    val items by viewModel.activeInvoiceItems.collectAsState()

    if (sale == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("কোনো ইনভয়েস নির্বাচন করা হয়নি")
        }
        return
    }

    val activeSale = sale!!
    val dateStr = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(activeSale.createdAt))

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header Controls
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text("ক্যাশ মেমো / বিক্রয় রশিদ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row {
                    IconButton(
                        onClick = {
                            val invoiceText = buildString {
                                append("মেমো নং: ${activeSale.invoiceNumber}\n")
                                append("দোকান: ${currentBusiness?.name ?: "আমার দোকান"}\n")
                                append("তারিখ: $dateStr\n")
                                append("কাস্টমার: ${activeSale.customerName}\n\n")
                                append("পণ্যসমূহ:\n")
                                items.forEach { itm ->
                                    append("- ${itm.productName} (${itm.quantity.toInt()} ${itm.unit}) = ৳${itm.subtotal.toInt()}\n")
                                }
                                append("\nমোট: ৳${activeSale.total.toInt()}\n")
                                append("পরিশোধ: ৳${activeSale.paid.toInt()}\n")
                                append("বকেয়া: ৳${activeSale.due.toInt()}\n\n")
                                append("ধন্যবাদ সাথে থাকার জন্য!")
                            }

                            if (activeSale.customerPhone.isNotEmpty()) {
                                viewModel.whatsAppService.sendInvoice(context, activeSale.customerPhone, invoiceText)
                            } else {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, invoiceText)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "মেমো শেয়ার করুন"))
                            }
                        },
                        modifier = Modifier.testTag("share_invoice_whatsapp")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Printable Invoice Body
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Shop Header
                    Text(
                        text = currentBusiness?.name ?: "আমার দোকান",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${currentBranch?.name ?: "প্রধান শাখা"} • ${currentBranch?.address ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "মোবাইল: ${(currentBranch?.phone ?: "").ifEmpty { currentBusiness?.phone ?: "" }}",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Invoice Metadata
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("মেমো নং: ${activeSale.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("তারিখ: $dateStr", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("কাস্টমার: ${activeSale.customerName}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        if (activeSale.customerPhone.isNotEmpty()) {
                            Text("মোবাইল: ${activeSale.customerPhone}", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Table Header
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                            Text("পণ্য", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("পরিমাণ", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("দর", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
                            Text("মোট", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
                        }
                    }

                    // Table Items
                    items.forEachIndexed { index, itm ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(itm.productName, modifier = Modifier.weight(2f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${itm.quantity.toInt()} ${itm.unit}", modifier = Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("৳${itm.unitPrice.toInt()}", modifier = Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.End)
                            Text("৳${itm.subtotal.toInt()}", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, textAlign = TextAlign.End)
                        }
                        if (index < items.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Calculations Table
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("সাবটোটাল:", fontSize = 13.sp)
                        Text("৳${activeSale.subtotal.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (activeSale.discount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ছাড় (Discount):", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                            Text("-৳${activeSale.discount.toInt()}", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("সর্বমোট বিল:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("৳${activeSale.total.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("পরিশোধিত (${activeSale.paymentMethod}):", fontSize = 13.sp)
                        Text("৳${activeSale.paid.toInt()}", fontWeight = FontWeight.Bold, color = SuccessGreen, fontSize = 14.sp)
                    }

                    if (activeSale.due > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("বর্তমান বকেয়া:", fontWeight = FontWeight.Bold, color = DueTextRed, fontSize = 14.sp)
                            Text("৳${activeSale.due.toInt()}", fontWeight = FontWeight.ExtraBold, color = DueTextRed, fontSize = 15.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "আমাদের সাথে থাকার জন্য ধন্যবাদ!\nপণ্য পরিবর্তনের ক্ষেত্রে অনুগ্রহ করে বিক্রয় মেমো সাথে রাখবেন।",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Button(
                onClick = { viewModel.navigateTo(AppScreen.POS) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("নতুন বিক্রি শুরু করুন")
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
