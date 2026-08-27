package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.entity.BusinessEntity
import com.example.data.entity.CustomerEntity
import com.example.data.entity.ProductEntity
import com.example.data.model.UserRole
import com.example.service.AuthService
import com.example.service.AuthorizationService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun read_app_name_string_from_context() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("আমার দোকান", appName)
    }

    @Test
    fun password_hashing_and_auth_verification() {
        val plainPassword = "Admin@Password123"
        val hash = AuthService.hashPassword(plainPassword)
        assertTrue(AuthService.verifyPassword(plainPassword, hash))
    }

    @Test
    fun test_database_crud_operations() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)

        val bizId = UUID.randomUUID().toString()
        val branchId = UUID.randomUUID().toString()

        val business = BusinessEntity(
            id = bizId,
            name = "টেস্ট দোকান",
            ownerName = "করিম সাহেব",
            phone = "01700000000",
            address = "মিরপুর, ঢাকা"
        )
        db.businessDao().insertBusiness(business)

        val product = ProductEntity(
            id = UUID.randomUUID().toString(),
            businessId = bizId,
            branchId = branchId,
            name = "চিনি (Sugar)",
            category = "মুদি",
            purchasePrice = 120.0,
            salePrice = 140.0,
            currentStock = 50.0,
            unit = "কেজি"
        )
        db.productDao().insertProduct(product)

        val products = db.productDao().getProductsByBranch(bizId, branchId).first()
        assertEquals(1, products.size)
        assertEquals("চিনি (Sugar)", products[0].name)
        assertEquals(50.0, products[0].currentStock, 0.01)

        val customer = CustomerEntity(
            id = UUID.randomUUID().toString(),
            businessId = bizId,
            branchId = branchId,
            name = "রহিম মিয়া",
            phone = "01800000000",
            totalDue = 500.0
        )
        db.customerDao().insertCustomer(customer)

        val customers = db.customerDao().getCustomersByBusiness(bizId).first()
        assertEquals(1, customers.size)
        assertEquals(500.0, customers[0].totalDue, 0.01)
    }
}
