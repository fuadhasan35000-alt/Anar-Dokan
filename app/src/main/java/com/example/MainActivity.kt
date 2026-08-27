package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.components.AppBottomBar
import com.example.ui.components.AppTopBar
import com.example.ui.components.BranchSelectorDialog
import com.example.ui.components.BusinessSelectorDialog
import com.example.ui.screens.*
import com.example.ui.theme.AmarDokanTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AmarDokanTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val currentScreen by mainViewModel.currentScreen.collectAsState()
                val currentUser by mainViewModel.currentUser.collectAsState()
                val currentBusiness by mainViewModel.currentBusiness.collectAsState()
                val currentBranch by mainViewModel.currentBranch.collectAsState()
                val allBusinesses by mainViewModel.allBusinesses.collectAsState()
                val branches by mainViewModel.currentBranches.collectAsState()

                var showBusinessDialog by remember { mutableStateOf(false) }
                var showBranchDialog by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    mainViewModel.snackbarMessage.collectLatest { msg ->
                        snackbarHostState.showSnackbar(msg)
                    }
                }

                // Global Back Navigation
                BackHandler(enabled = currentScreen != AppScreen.DASHBOARD && currentScreen != AppScreen.LOGIN && currentScreen != AppScreen.FIRST_RUN_SETUP && currentScreen != AppScreen.SPLASH) {
                    mainViewModel.navigateTo(AppScreen.DASHBOARD)
                }

                val showNavigationBars = currentUser != null && (
                        currentScreen == AppScreen.DASHBOARD ||
                                currentScreen == AppScreen.PRODUCTS ||
                                currentScreen == AppScreen.POS ||
                                currentScreen == AppScreen.CUSTOMERS ||
                                currentScreen == AppScreen.REPORTS
                        )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        if (showNavigationBars) {
                            AppTopBar(
                                currentBusiness = currentBusiness,
                                currentBranch = currentBranch,
                                currentUser = currentUser,
                                onSwitchBusinessClick = { showBusinessDialog = true },
                                onSwitchBranchClick = { showBranchDialog = true },
                                onSettingsClick = { mainViewModel.navigateTo(AppScreen.SETTINGS) },
                                onAiClick = { mainViewModel.navigateTo(AppScreen.AI_ASSISTANT) }
                            )
                        }
                    },
                    bottomBar = {
                        if (showNavigationBars) {
                            AppBottomBar(
                                currentScreen = currentScreen,
                                onNavigate = { screen -> mainViewModel.navigateTo(screen) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            AppScreen.SPLASH -> SplashScreen()
                            AppScreen.FIRST_RUN_SETUP -> FirstRunSetupScreen(viewModel = mainViewModel)
                            AppScreen.LOGIN -> LoginScreen(viewModel = mainViewModel)
                            AppScreen.DASHBOARD -> DashboardScreen(viewModel = mainViewModel)
                            AppScreen.PRODUCTS -> ProductsScreen(mainViewModel = mainViewModel)
                            AppScreen.POS -> PosScreen(mainViewModel = mainViewModel)
                            AppScreen.INVOICE -> InvoiceScreen(viewModel = mainViewModel)
                            AppScreen.CUSTOMERS -> CustomersScreen(mainViewModel = mainViewModel)
                            AppScreen.EXPENSES -> ExpensesScreen(mainViewModel = mainViewModel)
                            AppScreen.REPORTS -> ReportsScreen(mainViewModel = mainViewModel)
                            AppScreen.STAFF_MANAGEMENT -> StaffManagementScreen(mainViewModel = mainViewModel)
                            AppScreen.BUSINESS_MANAGEMENT -> BusinessManagementScreen(mainViewModel = mainViewModel)
                            AppScreen.AI_ASSISTANT -> AiAssistantScreen(mainViewModel = mainViewModel)
                            AppScreen.SETTINGS -> SettingsScreen(viewModel = mainViewModel)
                            AppScreen.BACKUP_RESTORE -> BackupRestoreScreen(viewModel = mainViewModel)
                            AppScreen.AUDIT_LOGS -> AuditLogsScreen(viewModel = mainViewModel)
                        }
                    }
                }

                // Global Business Switcher Dialog
                if (showBusinessDialog) {
                    BusinessSelectorDialog(
                        businesses = allBusinesses,
                        currentBusinessId = currentBusiness?.id,
                        onSelectBusiness = { selected ->
                            mainViewModel.switchBusiness(selected)
                            showBusinessDialog = false
                        },
                        onDismiss = { showBusinessDialog = false }
                    )
                }

                // Global Branch Switcher Dialog
                if (showBranchDialog) {
                    BranchSelectorDialog(
                        branches = branches,
                        currentBranchId = currentBranch?.id,
                        onSelectBranch = { selected ->
                            mainViewModel.switchBranch(selected)
                            showBranchDialog = false
                        },
                        onDismiss = { showBranchDialog = false }
                    )
                }
            }
        }
    }
}
