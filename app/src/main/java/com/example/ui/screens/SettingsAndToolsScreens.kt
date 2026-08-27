package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.AuditLogEntity
import com.example.data.model.UserRole
import com.example.service.AuthorizationService
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentBusiness by viewModel.currentBusiness.collectAsState()
    val currentBranch by viewModel.currentBranch.collectAsState()

    var isSyncing by remember { mutableStateOf(false) }

    val canManageStaff = AuthorizationService.canManageUsers(currentUser)
    val isSuperAdmin = currentUser?.role == UserRole.SUPER_ADMIN.name

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("সেটিংস ও প্রশাসন", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = currentUser?.name ?: "ব্যবহারকারী",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        )
                        Text(
                            text = "${currentUser?.email} • ${currentUser?.role}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "বর্তমান দোকান: ${currentBusiness?.name ?: ""} (${currentBranch?.name ?: ""})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            // Management Section
            item {
                Text("ব্যবসা ও টিম প্রশাসন", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsMenuItem(
                            icon = Icons.Default.Business,
                            title = "ব্যবসা ও শাখা নিয়ন্ত্রণ",
                            subtitle = "একাধিক দোকান ও শাখা পরিচালনা করুন",
                            onClick = { viewModel.navigateTo(AppScreen.BUSINESS_MANAGEMENT) }
                        )

                        if (canManageStaff) {
                            HorizontalDivider()
                            SettingsMenuItem(
                                icon = Icons.Default.PeopleOutline,
                                title = "স্টাফ ও রোল ম্যানেজমেন্ট",
                                subtitle = "স্টাফ অনুমোদন ও ভূমিকা নির্ধারণ",
                                onClick = { viewModel.navigateTo(AppScreen.STAFF_MANAGEMENT) }
                            )
                        }

                        HorizontalDivider()
                        SettingsMenuItem(
                            icon = Icons.Default.History,
                            title = "অডিট লগ (Audit Trails)",
                            subtitle = "সকল কার্যক্রমের অপরিবর্তনযোগ্য ইতিহাস",
                            onClick = { viewModel.navigateTo(AppScreen.AUDIT_LOGS) }
                        )
                    }
                }
            }

            // Cloud & Data Section
            item {
                Text("ডাটা ও ক্লাউড সিঙ্ক", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsMenuItem(
                            icon = Icons.Default.CloudSync,
                            title = "ফায়ারবেস ক্লাউড সিঙ্ক",
                            subtitle = if (isSyncing) "সিঙ্ক হচ্ছে..." else "অফলাইন ডাটা ক্লাউডে সিঙ্ক করুন",
                            trailing = {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            onClick = {
                                if (isSyncing) return@SettingsMenuItem
                                isSyncing = true
                                coroutineScope.launch {
                                    val count = viewModel.syncRepository.syncPendingOperations()
                                    isSyncing = false
                                    viewModel.showSnackbar("ক্লাউড সিঙ্ক সম্পন্ন: $count টি পরিবর্তন সংরক্ষিত")
                                }
                            }
                        )

                        HorizontalDivider()
                        SettingsMenuItem(
                            icon = Icons.Default.Backup,
                            title = "ডাটা ব্যাকআপ ও রিস্টোর",
                            subtitle = "JSON ফাইল এক্সপোর্ট এবং ইমপোর্ট",
                            onClick = { viewModel.navigateTo(AppScreen.BACKUP_RESTORE) }
                        )

                        HorizontalDivider()
                        SettingsMenuItem(
                            icon = Icons.Default.AddCircleOutline,
                            title = "ডেমো ডাটা যুক্ত করুন (Seed Data)",
                            subtitle = "পরীক্ষার জন্য কিছু নমুনা পণ্য ও হিসাব যোগ করুন",
                            onClick = { viewModel.seedDemoData() }
                        )
                    }
                }
            }

            // Logout Section
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("logout_button")
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("অ্যাকাউন্ট থেকে লগআউট করুন")
                }
            }
        }
    }
}

@Composable
fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (trailing != null) {
            trailing()
        } else {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val currentBusiness by viewModel.currentBusiness.collectAsState()
    val currentBranch by viewModel.currentBranch.collectAsState()

    val businessId = currentBusiness?.id ?: ""
    val branchId = currentBranch?.id ?: ""

    var isProcessing by remember { mutableStateOf(false) }

    // SAF Create Document for Export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && businessId.isNotEmpty()) {
            isProcessing = true
            coroutineScope.launch {
                val res = viewModel.backupRestoreService.writeBackupToUri(uri, businessId)
                isProcessing = false
                res.fold(
                    onSuccess = { viewModel.showSnackbar("ডাটা ব্যাকআপ সফলভাবে এক্সপোর্ট হয়েছে!") },
                    onFailure = { err -> viewModel.showSnackbar("ব্যাকআপ ব্যর্থ: ${err.localizedMessage}") }
                )
            }
        }
    }

    // SAF Open Document for Import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && businessId.isNotEmpty() && branchId.isNotEmpty()) {
            isProcessing = true
            coroutineScope.launch {
                val res = viewModel.backupRestoreService.restoreBackupFromUri(uri, businessId, branchId)
                isProcessing = false
                res.fold(
                    onSuccess = { count -> viewModel.showSnackbar("$count টি রেকর্ড সফলভাবে রিস্টোর হয়েছে!") },
                    onFailure = { err -> viewModel.showSnackbar("রিস্টোর ব্যর্থ: ${err.localizedMessage}") }
                )
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("ডাটা ব্যাকআপ ও রিস্টোর", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.SETTINGS) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("১. ডাটা ব্যাকআপ এক্সপোর্ট", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "আপনার বর্তমান দোকানের সকল পণ্য, কাস্টমার, বিক্রি ও খরচের হিসাব একটি নিরাপদ JSON ফাইল আকারে ডিভাইসে সংরক্ষণ করুন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val fileName = "AmarDokan_Backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.json"
                            exportLauncher.launch(fileName)
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth().testTag("export_backup_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ব্যাকআপ ফাইল ডাউনলোড করুন")
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("২. ব্যাকআপ থেকে রিস্টোর", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "পূর্বে এক্সপোর্ট করা JSON ব্যাকআপ ফাইল থেকে সকল ডাটা বর্তমান দোকানে ফিরিয়ে আনুন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth().testTag("import_backup_button")
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ব্যাকআপ ফাইল সিলেক্ট ও রিস্টোর করুন")
                    }
                }
            }

            if (isProcessing) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentBusiness by viewModel.currentBusiness.collectAsState()
    val businessId = currentBusiness?.id ?: ""

    val logs by remember(businessId) {
        if (businessId.isNotEmpty()) {
            viewModel.db.auditLogDao().getLogsByBusiness(businessId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("সিস্টেম অডিট লগ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.SETTINGS) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("কোনো অডিট রেকর্ড পাওয়া যায়নি")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()).format(Date(log.timestamp))

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = log.action,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(log.details, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("সম্পাদনকারী: ${log.userName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
