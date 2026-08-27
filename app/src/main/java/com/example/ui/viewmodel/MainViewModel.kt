package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.DatabaseProvider
import com.example.data.entity.*
import com.example.data.model.*
import com.example.service.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class AppScreen {
    SPLASH,
    FIRST_RUN_SETUP,
    LOGIN,
    DASHBOARD,
    PRODUCTS,
    POS,
    INVOICE,
    CUSTOMERS,
    EXPENSES,
    REPORTS,
    STAFF_MANAGEMENT,
    BUSINESS_MANAGEMENT,
    AI_ASSISTANT,
    SETTINGS,
    BACKUP_RESTORE,
    AUDIT_LOGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val db: AppDatabase = DatabaseProvider.getDatabase(application)
    val authService: AuthService = AuthService(db, application)
    val syncRepository: SyncRepository = SyncRepository(db, application)
    val smsService: SmsService = SmsService()
    val whatsAppService: WhatsAppService = WhatsAppService()
    val backupRestoreService: BackupRestoreService = BackupRestoreService(db, application)
    val aiDataSummaryService: AiDataSummaryService = AiDataSummaryService(db)
    val aiAssistantService: AiAssistantService = AiAssistantService(aiDataSummaryService)

    // Current State
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.SPLASH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _currentBusiness = MutableStateFlow<BusinessEntity?>(null)
    val currentBusiness: StateFlow<BusinessEntity?> = _currentBusiness.asStateFlow()

    private val _currentBranch = MutableStateFlow<BranchEntity?>(null)
    val currentBranch: StateFlow<BranchEntity?> = _currentBranch.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _activeInvoice = MutableStateFlow<SaleEntity?>(null)
    val activeInvoice: StateFlow<SaleEntity?> = _activeInvoice.asStateFlow()

    private val _activeInvoiceItems = MutableStateFlow<List<SaleItemEntity>>(emptyList())
    val activeInvoiceItems: StateFlow<List<SaleItemEntity>> = _activeInvoiceItems.asStateFlow()

    // Businesses and Branches lists
    val allBusinesses: StateFlow<List<BusinessEntity>> = db.businessDao().getAllBusinesses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentBranches: StateFlow<List<BranchEntity>> = _currentBusiness
        .flatMapLatest { b ->
            if (b != null) db.branchDao().getActiveBranches(b.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkInitialAppState()
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun showSnackbar(message: String) {
        viewModelScope.launch {
            _snackbarMessage.emit(message)
        }
    }

    fun checkInitialAppState() {
        viewModelScope.launch {
            val superAdminCount = db.userDao().getSuperAdminCount()
            if (superAdminCount == 0) {
                _currentScreen.value = AppScreen.FIRST_RUN_SETUP
            } else {
                val user = authService.getCurrentUser()
                if (user != null && AuthorizationService.isUserApproved(user)) {
                    _currentUser.value = user
                    loadBusinessAndBranch(user.businessId, user.branchId)
                    _currentScreen.value = AppScreen.DASHBOARD
                } else {
                    _currentScreen.value = AppScreen.LOGIN
                }
            }
        }
    }

    suspend fun loadBusinessAndBranch(businessId: String, branchId: String) = withContext(Dispatchers.IO) {
        val business = db.businessDao().getBusinessById(businessId)
        val branch = db.branchDao().getBranchById(branchId) ?: db.branchDao().getBranchesByBusiness(businessId).firstOrNull()?.firstOrNull()

        _currentBusiness.value = business
        _currentBranch.value = branch
        authService.saveActiveBusinessAndBranch(businessId, branch?.id ?: branchId)
    }

    fun switchBusiness(business: BusinessEntity) {
        viewModelScope.launch {
            val branches = db.branchDao().getActiveBranches(business.id).firstOrNull() ?: emptyList()
            val firstBranch = branches.firstOrNull()
            _currentBusiness.value = business
            _currentBranch.value = firstBranch
            authService.saveActiveBusinessAndBranch(business.id, firstBranch?.id ?: "")
            showSnackbar("'${business.name}' ব্যবসায় সুইচ করা হয়েছে")
        }
    }

    fun switchBranch(branch: BranchEntity) {
        viewModelScope.launch {
            _currentBranch.value = branch
            val bId = _currentBusiness.value?.id ?: branch.businessId
            authService.saveActiveBusinessAndBranch(bId, branch.id)
            showSnackbar("'${branch.name}' শাখায় পরিবর্তন করা হয়েছে")
        }
    }

    fun logout() {
        viewModelScope.launch {
            authService.logout(_currentUser.value)
            _currentUser.value = null
            _currentBusiness.value = null
            _currentBranch.value = null
            _currentScreen.value = AppScreen.LOGIN
            showSnackbar("সফলভাবে লগআউট হয়েছে")
        }
    }

    fun viewInvoice(sale: SaleEntity) {
        viewModelScope.launch {
            _activeInvoice.value = sale
            _activeInvoiceItems.value = db.saleDao().getSaleItemsDirect(sale.id)
            _currentScreen.value = AppScreen.INVOICE
        }
    }

    fun seedDemoData() {
        viewModelScope.launch(Dispatchers.IO) {
            val business = _currentBusiness.value ?: return@launch
            val branch = _currentBranch.value ?: return@launch
            val user = _currentUser.value ?: return@launch

            // Seed Products
            val sampleProducts = listOf(
                ProductEntity(
                    businessId = business.id,
                    branchId = branch.id,
                    name = "মিনিকেট চাল (৫০ কেজি)",
                    category = "মুদি",
                    purchasePrice = 3200.0,
                    salePrice = 3500.0,
                    currentStock = 25.0,
                    minimumStock = 5.0,
                    unit = "বস্তা",
                    sku = "RICE-50KG",
                    barcode = "89411001"
                ),
                ProductEntity(
                    businessId = business.id,
                    branchId = branch.id,
                    name = "সয়াবিন তেল (৫ লিটার)",
                    category = "মুদি",
                    purchasePrice = 780.0,
                    salePrice = 850.0,
                    currentStock = 40.0,
                    minimumStock = 8.0,
                    unit = "বোতল",
                    sku = "OIL-5L",
                    barcode = "89411002"
                ),
                ProductEntity(
                    businessId = business.id,
                    branchId = branch.id,
                    name = "চিনি (১ কেজি)",
                    category = "মুদি",
                    purchasePrice = 120.0,
                    salePrice = 135.0,
                    currentStock = 4.0, // Low stock demo!
                    minimumStock = 10.0,
                    unit = "কেজি",
                    sku = "SUGAR-1KG",
                    barcode = "89411003"
                ),
                ProductEntity(
                    businessId = business.id,
                    branchId = branch.id,
                    name = "মসুর ডাল (১ কেজি)",
                    category = "মুদি",
                    purchasePrice = 130.0,
                    salePrice = 145.0,
                    currentStock = 50.0,
                    minimumStock = 10.0,
                    unit = "কেজি",
                    sku = "LENTIL-1KG",
                    barcode = "89411004"
                ),
                ProductEntity(
                    businessId = business.id,
                    branchId = branch.id,
                    name = "লাক্স সাবান ১০০ গ্রাম",
                    category = "কসমেটিক্স",
                    purchasePrice = 45.0,
                    salePrice = 55.0,
                    currentStock = 3.0, // Low stock demo!
                    minimumStock = 12.0,
                    unit = "পিস",
                    sku = "SOAP-LUX",
                    barcode = "89411005"
                )
            )

            sampleProducts.forEach { db.productDao().insertProduct(it) }

            // Seed Customers
            val sampleCustomers = listOf(
                CustomerEntity(
                    businessId = business.id,
                    branchId = branch.id,
                    name = "মোঃ রফিকুল ইসলাম",
                    phone = "01711223344",
                    address = "রোড ৪, হাউস ১২, ঢাকা",
                    totalPurchase = 12500.0,
                    totalPaid = 8500.0,
                    totalDue = 4000.0
                ),
                CustomerEntity(
                    businessId = business.id,
                    branchId = branch.id,
                    name = "আব্দুর রহমান",
                    phone = "01819887766",
                    address = "বাজার মোড়",
                    totalPurchase = 8500.0,
                    totalPaid = 7000.0,
                    totalDue = 1500.0
                ),
                CustomerEntity(
                    businessId = business.id,
                    branchId = branch.id,
                    name = "আফরোজা বেগম",
                    phone = "01912345678",
                    address = "কলেজ রোড",
                    totalPurchase = 15400.0,
                    totalPaid = 15400.0,
                    totalDue = 0.0
                )
            )

            sampleCustomers.forEach { db.customerDao().insertCustomer(it) }

            // Seed an Expense
            db.expenseDao().insertExpense(
                ExpenseEntity(
                    businessId = business.id,
                    branchId = branch.id,
                    title = "দোকান বিদ্যুৎ বিল",
                    category = "বিদ্যুৎ বিল",
                    amount = 1850.0,
                    note = "চলতি মাসের বিদ্যুৎ বিল পরিশোধ",
                    createdBy = user.name
                )
            )

            showSnackbar("ডেমো ডাটা সফলভাবে যুক্ত করা হয়েছে!")
        }
    }
}
