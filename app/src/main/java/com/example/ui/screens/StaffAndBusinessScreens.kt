package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.BranchEntity
import com.example.data.entity.BusinessEntity
import com.example.data.entity.UserEntity
import com.example.data.model.ShopPermission
import com.example.data.model.UserRole
import com.example.data.model.UserStatus
import com.example.service.AuthorizationService
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.StaffViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val staffViewModel = remember { StaffViewModel(mainViewModel.db) }

    val currentBusiness by mainViewModel.currentBusiness.collectAsState()
    val currentBranch by mainViewModel.currentBranch.collectAsState()
    val currentUser by mainViewModel.currentUser.collectAsState()
    val branches by mainViewModel.currentBranches.collectAsState()

    val businessId = currentBusiness?.id ?: ""
    val branchId = currentBranch?.id ?: ""

    val users by remember(businessId) {
        if (businessId.isNotEmpty()) {
            staffViewModel.getUsersByBusiness(businessId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    val pendingUsers = remember(users) { users.filter { it.status == UserStatus.PENDING.name } }
    val activeUsers = remember(users) { users.filter { it.status != UserStatus.PENDING.name } }

    var showAddStaffDialog by remember { mutableStateOf(false) }
    var userForPermissions by remember { mutableStateOf<UserEntity?>(null) }

    val canManage = AuthorizationService.canManageUsers(currentUser)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("স্টাফ ও অনুমতি ব্যবস্থাপনা", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { mainViewModel.navigateTo(AppScreen.DASHBOARD) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (canManage) {
                ExtendedFloatingActionButton(
                    onClick = { showAddStaffDialog = true },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("নতুন স্টাফ যোগ") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_staff_fab")
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Pending Approvals Section
            if (pendingUsers.isNotEmpty()) {
                item {
                    Text(
                        text = "অনুমোদনের অপেক্ষায় (${pendingUsers.size} জন)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber
                        )
                    )
                }

                items(pendingUsers) { pending ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pending.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("ইমেইল: ${pending.email}", style = MaterialTheme.typography.bodySmall)
                                    Text("মোবাইল: ${pending.phone}", style = MaterialTheme.typography.bodySmall)
                                }
                                Surface(
                                    color = WarningAmber,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "অপেক্ষমান",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val admin = currentUser ?: return@launch
                                            staffViewModel.rejectStaff(pending, admin)
                                            mainViewModel.showSnackbar("${pending.name} এর আবেদন বাতিল করা হয়েছে")
                                        }
                                    }
                                ) {
                                    Text("বাতিল করুন", color = MaterialTheme.colorScheme.error)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val admin = currentUser ?: return@launch
                                            staffViewModel.approveStaff(pending, admin)
                                            mainViewModel.showSnackbar("${pending.name} কে অনুমোদন দেওয়া হয়েছে")
                                        }
                                    },
                                    modifier = Modifier.testTag("approve_staff_button")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("অনুমোদন করুন")
                                }
                            }
                        }
                    }
                }
            }

            // 2. Active Staff List
            item {
                Text(
                    text = "সকল সদস্য (${activeUsers.size} জন)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            items(activeUsers) { user ->
                val isApproved = user.status == UserStatus.APPROVED.name
                val roleTitle = when (user.role) {
                    UserRole.SUPER_ADMIN.name -> "সুপার অ্যাডমিন"
                    UserRole.ADMIN.name -> "অ্যাডমিন"
                    else -> "স্টাফ"
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(user.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = if (user.role == UserRole.SUPER_ADMIN.name) EmeraldPrimaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = roleTitle,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text("${user.email} • ${user.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            // Status badge
                            Surface(
                                color = if (isApproved) SuccessGreen else MaterialTheme.colorScheme.error,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isApproved) "সক্রিয়" else "নিষ্ক্রিয়",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        if (user.role == UserRole.STAFF.name && canManage) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { userForPermissions = user }) {
                                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("অনুমতি পরিবর্তন (Permissions)", fontSize = 12.sp)
                                }

                                TextButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val admin = currentUser ?: return@launch
                                            staffViewModel.toggleUserActive(user, admin)
                                            mainViewModel.showSnackbar("${user.name} এর স্ট্যাটাস পরিবর্তন হয়েছে")
                                        }
                                    }
                                ) {
                                    Text(
                                        text = if (isApproved) "নিষ্ক্রিয় করুন" else "সক্রিয় করুন",
                                        color = if (isApproved) MaterialTheme.colorScheme.error else SuccessGreen,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Staff Dialog
    if (showAddStaffDialog) {
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var selectedRole by remember { mutableStateOf(UserRole.STAFF) }
        var selectedBranchId by remember { mutableStateOf(branches.firstOrNull()?.id ?: branchId) }
        var selectedPermissions by remember { mutableStateOf(AuthorizationService.getDefaultStaffPermissions().toSet()) }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = { showAddStaffDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("নতুন সদস্য যুক্ত করুন", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("নাম *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("staff_name_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("ইমেইল *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("staff_email_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("মোবাইল নম্বর *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("পাসওয়ার্ড *") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("staff_password_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("ভূমিকা (Role):", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(UserRole.STAFF, UserRole.ADMIN).forEach { r ->
                            FilterChip(
                                selected = selectedRole == r,
                                onClick = { selectedRole = r },
                                label = { Text(r.banglaTitle) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddStaffDialog = false }) {
                            Text("বাতিল")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (name.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank()) {
                                    errorMsg = "সকল তথ্য পূরণ করুন"
                                    return@Button
                                }
                                val creator = currentUser ?: return@Button
                                coroutineScope.launch {
                                    val res = staffViewModel.createStaffOrAdmin(
                                        name = name,
                                        email = email,
                                        phone = phone,
                                        password = password,
                                        role = selectedRole,
                                        businessId = businessId,
                                        branchId = selectedBranchId,
                                        assignedPermissions = selectedPermissions.toList(),
                                        creatorUser = creator
                                    )
                                    res.fold(
                                        onSuccess = {
                                            showAddStaffDialog = false
                                            mainViewModel.showSnackbar("ব্যবহারকারী সফলভাবে তৈরি হয়েছে")
                                        },
                                        onFailure = { err -> errorMsg = err.localizedMessage }
                                    )
                                }
                            },
                            modifier = Modifier.testTag("save_staff_button")
                        ) {
                            Text("সেভ করুন")
                        }
                    }
                }
            }
        }
    }

    // Edit Permissions Dialog
    if (userForPermissions != null) {
        val targetUser = userForPermissions!!
        val userPerms = remember(targetUser) {
            mutableStateListOf<String>().apply {
                addAll(targetUser.permissionsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() })
            }
        }

        AlertDialog(
            onDismissRequest = { userForPermissions = null },
            title = { Text("${targetUser.name} - অনুমতি নির্ধারণ", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(ShopPermission.values()) { perm ->
                        val isChecked = userPerms.contains(perm.name)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) {
                                        userPerms.remove(perm.name)
                                    } else {
                                        userPerms.add(perm.name)
                                    }
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (!userPerms.contains(perm.name)) userPerms.add(perm.name)
                                    } else {
                                        userPerms.remove(perm.name)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(perm.banglaTitle, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(perm.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val admin = currentUser ?: return@Button
                        coroutineScope.launch {
                            val selectedList = ShopPermission.values().filter { userPerms.contains(it.name) }
                            staffViewModel.updatePermissions(targetUser, selectedList, admin)
                            userForPermissions = null
                            mainViewModel.showSnackbar("অনুমতিসমূহ সফলভাবে সংরক্ষিত হয়েছে")
                        }
                    }
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { userForPermissions = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessManagementScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val allBusinesses by mainViewModel.allBusinesses.collectAsState()
    val currentBusiness by mainViewModel.currentBusiness.collectAsState()
    val branches by mainViewModel.currentBranches.collectAsState()
    val currentUser by mainViewModel.currentUser.collectAsState()

    var showAddBusinessDialog by remember { mutableStateOf(false) }
    var showAddBranchDialog by remember { mutableStateOf(false) }

    val isSuperAdmin = currentUser?.role == UserRole.SUPER_ADMIN.name
    val isAdmin = currentUser?.role == UserRole.ADMIN.name || isSuperAdmin

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("ব্যবসা ও শাখা ব্যবস্থাপনা", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { mainViewModel.navigateTo(AppScreen.SETTINGS) }) {
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
            // 1. Businesses Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("সকল ব্যবসা (${allBusinesses.size} টি)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    if (isSuperAdmin) {
                        TextButton(onClick = { showAddBusinessDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("নতুন ব্যবসা")
                        }
                    }
                }
            }

            items(allBusinesses) { biz ->
                val isSelected = biz.id == currentBusiness?.id
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { mainViewModel.switchBusiness(biz) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(biz.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("মালিক: ${biz.ownerName} • ${biz.phone}", style = MaterialTheme.typography.bodySmall)
                            Text(biz.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // 2. Branches under active business
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("বর্তমান ব্যবসার শাখাসমূহ (${branches.size} টি)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    if (isAdmin) {
                        TextButton(onClick = { showAddBranchDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("নতুন শাখা")
                        }
                    }
                }
            }

            items(branches) { br ->
                val isSelected = br.id == mainViewModel.currentBranch.value?.id
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { mainViewModel.switchBranch(br) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(br.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("ঠিকানা: ${br.address}", style = MaterialTheme.typography.bodySmall)
                            if (br.phone.isNotEmpty()) Text("ফোন: ${br.phone}", style = MaterialTheme.typography.bodySmall)
                        }

                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }

    // Add Business Dialog
    if (showAddBusinessDialog) {
        var name by remember { mutableStateOf("") }
        var owner by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddBusinessDialog = false },
            title = { Text("নতুন ব্যবসা তৈরি", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("ব্যবসার নাম *") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = owner, onValueChange = { owner = it }, label = { Text("মালিকের নাম *") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("মোবাইল *") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("ঠিকানা") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isBlank() || owner.isBlank() || phone.isBlank()) return@Button
                    val bId = UUID.randomUUID().toString()
                    val branchId = UUID.randomUUID().toString()
                    val newBiz = BusinessEntity(id = bId, name = name.trim(), ownerName = owner.trim(), phone = phone.trim(), address = address.trim())
                    val mainBr = BranchEntity(id = branchId, businessId = bId, name = "প্রধান শাখা (Main)", address = address.trim(), phone = phone.trim())

                    coroutineScope.launch {
                        mainViewModel.db.businessDao().insertBusiness(newBiz)
                        mainViewModel.db.branchDao().insertBranch(mainBr)
                        showAddBusinessDialog = false
                        mainViewModel.showSnackbar("নতুন ব্যবসা '${name}' সফলভাবে তৈরি হয়েছে")
                    }
                }) {
                    Text("তৈরি করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBusinessDialog = false }) { Text("বাতিল") }
            }
        )
    }

    // Add Branch Dialog
    if (showAddBranchDialog) {
        val bizId = currentBusiness?.id ?: ""
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddBranchDialog = false },
            title = { Text("নতুন শাখা যুক্ত করুন", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("শাখার নাম *") }, placeholder = { Text("যেমন: গুলশান শাখা") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("শাখার ঠিকানা *") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("ফোন নম্বর") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isBlank() || address.isBlank()) return@Button
                    val br = BranchEntity(id = UUID.randomUUID().toString(), businessId = bizId, name = name.trim(), address = address.trim(), phone = phone.trim())
                    coroutineScope.launch {
                        mainViewModel.db.branchDao().insertBranch(br)
                        showAddBranchDialog = false
                        mainViewModel.showSnackbar("নতুন শাখা '${name}' যুক্ত হয়েছে")
                    }
                }) {
                    Text("শাখা যুক্ত করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBranchDialog = false }) { Text("বাতিল") }
            }
        )
    }
}
