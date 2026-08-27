package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.BranchEntity
import com.example.data.entity.BusinessEntity
import com.example.data.entity.UserEntity
import com.example.data.model.UserRole
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldTertiary
import com.example.ui.viewmodel.AppScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    currentBusiness: BusinessEntity?,
    currentBranch: BranchEntity?,
    currentUser: UserEntity?,
    onSwitchBusinessClick: () -> Unit,
    onSwitchBranchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAiClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Business & Branch Info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSwitchBusinessClick() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentBusiness?.name ?: "আমার দোকান",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "ব্যবসা পরিবর্তন",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSwitchBranchClick() }
                            .padding(vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentBranch?.name ?: "প্রধান শাখা",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "শাখা পরিবর্তন",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // AI Assistant & Settings Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onAiClick,
                        modifier = Modifier
                            .testTag("ai_assistant_button")
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "দোকান AI",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "সেটিংস",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // User Role Chip banner
            if (currentUser != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E7D32))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentUser.name,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    val roleTitle = when (currentUser.role) {
                        UserRole.SUPER_ADMIN.name -> "সুপার অ্যাডমিন"
                        UserRole.ADMIN.name -> "অ্যাডমিন"
                        else -> "স্টাফ"
                    }
                    val roleBg = when (currentUser.role) {
                        UserRole.SUPER_ADMIN.name -> MaterialTheme.colorScheme.primaryContainer
                        UserRole.ADMIN.name -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val roleTextColor = when (currentUser.role) {
                        UserRole.SUPER_ADMIN.name -> MaterialTheme.colorScheme.onPrimaryContainer
                        UserRole.ADMIN.name -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Surface(
                        color = roleBg,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = roleTitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = roleTextColor
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppBottomBar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == AppScreen.DASHBOARD,
            onClick = { onNavigate(AppScreen.DASHBOARD) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == AppScreen.DASHBOARD) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "হোম"
                )
            },
            label = { Text("হোম", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            modifier = Modifier.testTag("nav_home")
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.PRODUCTS,
            onClick = { onNavigate(AppScreen.PRODUCTS) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == AppScreen.PRODUCTS) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2,
                    contentDescription = "স্টক"
                )
            },
            label = { Text("স্টক", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            modifier = Modifier.testTag("nav_stock")
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.POS,
            onClick = { onNavigate(AppScreen.POS) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == AppScreen.POS) Icons.Filled.PointOfSale else Icons.Outlined.PointOfSale,
                    contentDescription = "বিক্রি"
                )
            },
            label = { Text("বিক্রি", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag("nav_sales")
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.CUSTOMERS,
            onClick = { onNavigate(AppScreen.CUSTOMERS) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == AppScreen.CUSTOMERS) Icons.Filled.PeopleAlt else Icons.Outlined.PeopleAlt,
                    contentDescription = "কাস্টমার"
                )
            },
            label = { Text("কাস্টমার", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            modifier = Modifier.testTag("nav_customers")
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.REPORTS,
            onClick = { onNavigate(AppScreen.REPORTS) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == AppScreen.REPORTS) Icons.Filled.Assessment else Icons.Outlined.Assessment,
                    contentDescription = "রিপোর্ট"
                )
            },
            label = { Text("রিপোর্ট", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            modifier = Modifier.testTag("nav_reports")
        )
    }
}
