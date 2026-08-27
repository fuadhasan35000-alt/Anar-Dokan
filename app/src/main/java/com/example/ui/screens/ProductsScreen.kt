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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ProductEntity
import com.example.data.model.ShopPermission
import com.example.service.AuthorizationService
import com.example.ui.components.AddEditProductDialog
import com.example.ui.components.StockAdjustDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val productViewModel = remember { ProductViewModel(mainViewModel.db) }

    val currentBusiness by mainViewModel.currentBusiness.collectAsState()
    val currentBranch by mainViewModel.currentBranch.collectAsState()
    val currentUser by mainViewModel.currentUser.collectAsState()

    val businessId = currentBusiness?.id ?: ""
    val branchId = currentBranch?.id ?: ""

    val products by remember(businessId, branchId) {
        if (businessId.isNotEmpty() && branchId.isNotEmpty()) {
            productViewModel.getProducts(businessId, branchId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    val searchQuery by productViewModel.searchQuery.collectAsState()
    val selectedCategory by productViewModel.selectedCategory.collectAsState()
    val filterLowStockOnly by productViewModel.filterLowStockOnly.collectAsState()

    // Dialog state
    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var productToAdjustStock by remember { mutableStateOf<ProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }

    val canAddProduct = AuthorizationService.hasPermission(currentUser, ShopPermission.ADD_PRODUCT)
    val canEditProduct = AuthorizationService.hasPermission(currentUser, ShopPermission.EDIT_PRODUCT)
    val canEditStock = AuthorizationService.hasPermission(currentUser, ShopPermission.EDIT_STOCK)
    val canDeleteProduct = AuthorizationService.hasPermission(currentUser, ShopPermission.DELETE_PRODUCT)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (canAddProduct) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("নতুন পণ্য", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_product_fab")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { productViewModel.setSearchQuery(it) },
                        placeholder = { Text("পণ্যের নাম, বারকোড বা SKU দিয়ে খুঁজুন...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { productViewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_search_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filters row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null && !filterLowStockOnly,
                                onClick = {
                                    productViewModel.setSelectedCategory(null)
                                    if (filterLowStockOnly) productViewModel.toggleLowStockFilter()
                                },
                                label = { Text("সব পণ্য (${products.size})") }
                            )
                        }

                        item {
                            FilterChip(
                                selected = filterLowStockOnly,
                                onClick = { productViewModel.toggleLowStockFilter() },
                                label = { Text("⚠️ কম স্টক") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DueCardRed,
                                    selectedLabelColor = DueTextRed
                                )
                            )
                        }

                        val categories = listOf("মুদি", "কসমেটিক্স", "ইলেকট্রনিক্স", "গার্মেন্টস", "ঔষধ", "অন্যান্য")
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = {
                                    productViewModel.setSelectedCategory(if (selectedCategory == cat) null else cat)
                                },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
            }

            // Products List
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "কোনো পণ্য পাওয়া যায়নি" else "স্টকে কোনো পণ্য যুক্ত করা নেই",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (canAddProduct && searchQuery.isEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { showAddDialog = true }) {
                                Text("প্রথম পণ্য যোগ করুন")
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
                    items(products, key = { it.id }) { product ->
                        val isLowStock = product.currentStock <= product.minimumStock

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isLowStock) DueCardRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = product.name,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = "ক্যাটাগরি: ${product.category}" + if (product.sku.isNotEmpty()) " • SKU: ${product.sku}" else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Stock pill badge
                                    Surface(
                                        color = if (isLowStock) DueTextRed else SuccessGreen,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "স্টক: ${product.currentStock.toInt()} ${product.unit}",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Prices
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Column {
                                            Text("বিক্রয় মূল্য", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("৳${product.salePrice.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
                                        }
                                        Column {
                                            Text("ক্রয় মূল্য", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("৳${product.purchasePrice.toInt()}", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        }
                                    }

                                    // Actions
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (canEditStock) {
                                            OutlinedButton(
                                                onClick = { productToAdjustStock = product },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("স্টক ইন/আউট", fontSize = 12.sp)
                                            }
                                        }

                                        if (canEditProduct) {
                                            IconButton(
                                                onClick = { productToEdit = product },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                            }
                                        }

                                        if (canDeleteProduct) {
                                            IconButton(
                                                onClick = { productToDelete = product },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(18.dp))
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

    // Add Product Dialog
    if (showAddDialog) {
        AddEditProductDialog(
            businessId = businessId,
            branchId = branchId,
            onSave = { product ->
                coroutineScope.launch {
                    val user = currentUser ?: return@launch
                    productViewModel.addOrUpdateProduct(product, user.id, user.name)
                    showAddDialog = false
                    mainViewModel.showSnackbar("পণ্য যুক্ত করা হয়েছে: ${product.name}")
                }
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // Edit Product Dialog
    if (productToEdit != null) {
        AddEditProductDialog(
            initialProduct = productToEdit,
            businessId = businessId,
            branchId = branchId,
            onSave = { updated ->
                coroutineScope.launch {
                    val user = currentUser ?: return@launch
                    productViewModel.addOrUpdateProduct(updated, user.id, user.name)
                    productToEdit = null
                    mainViewModel.showSnackbar("পণ্য আপডেট করা হয়েছে: ${updated.name}")
                }
            },
            onDismiss = { productToEdit = null }
        )
    }

    // Adjust Stock Dialog
    if (productToAdjustStock != null) {
        val prod = productToAdjustStock!!
        val user = currentUser
        if (user != null) {
            StockAdjustDialog(
                product = prod,
                onAdjust = { type, qty, note ->
                    coroutineScope.launch {
                        productViewModel.adjustStock(prod, type, qty, note, user.id, user.name)
                        productToAdjustStock = null
                        mainViewModel.showSnackbar("স্টক আপডেট সফল হয়েছে")
                    }
                },
                onDismiss = { productToAdjustStock = null }
            )
        }
    }

    // Delete Confirmation Dialog
    if (productToDelete != null) {
        val prod = productToDelete!!
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("পণ্য মুছে ফেলতে চান?") },
            text = { Text("'${prod.name}' ক্যাটালগ থেকে মুছে ফেলা হবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val user = currentUser ?: return@launch
                            productViewModel.deleteProduct(prod, user.id, user.name)
                            productToDelete = null
                            mainViewModel.showSnackbar("পণ্য মুছে ফেলা হয়েছে")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("হ্যাঁ, ডিলিট করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
