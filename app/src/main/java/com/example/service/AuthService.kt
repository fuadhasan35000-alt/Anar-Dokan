package com.example.service

import android.content.Context
import android.content.SharedPreferences
import com.example.data.database.AppDatabase
import com.example.data.entity.AuditLogEntity
import com.example.data.entity.UserEntity
import com.example.data.model.AuditAction
import com.example.data.model.UserRole
import com.example.data.model.UserStatus
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

class AuthService(
    private val db: AppDatabase,
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("amar_dokan_auth_session", Context.MODE_PRIVATE)
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    companion object {
        private const val KEY_CURRENT_USER_ID = "current_user_id"
        private const val KEY_CURRENT_BUSINESS_ID = "current_business_id"
        private const val KEY_CURRENT_BRANCH_ID = "current_branch_id"
    }

    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    suspend fun login(email: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val user = db.userDao().getUserByEmail(email.trim().lowercase())
                ?: return@withContext Result.failure(Exception("ব্যবহারকারী খুঁজে পাওয়া যায়নি। ইমেইল চেক করুন।"))

            val inputHash = hashPassword(password)
            if (user.passwordHash != inputHash) {
                return@withContext Result.failure(Exception("ভুল পাসওয়ার্ড। আবার চেষ্টা করুন।"))
            }

            if (user.status == UserStatus.PENDING.name) {
                return@withContext Result.failure(Exception("আপনার অ্যাকাউন্টটি এখনো অনুমোদিত হয়নি। অ্যাডমিনের অনুমোদনের জন্য অপেক্ষা করুন।"))
            }

            if (user.status == UserStatus.REJECTED.name) {
                return@withContext Result.failure(Exception("আপনার অ্যাকাউন্ট আবেদনটি বাতিল করা হয়েছে।"))
            }

            if (user.status == UserStatus.DISABLED.name) {
                return@withContext Result.failure(Exception("আপনার অ্যাকাউন্টটি নিষ্ক্রিয় করা হয়েছে। অ্যাডমিনের সাথে যোগাযোগ করুন।"))
            }

            // Cloud auth synchronization attempt if online
            try {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
                }
            } catch (e: Exception) {
                // Offline fallback works smoothly
            }

            // Save active session
            prefs.edit()
                .putString(KEY_CURRENT_USER_ID, user.id)
                .putString(KEY_CURRENT_BUSINESS_ID, user.businessId)
                .putString(KEY_CURRENT_BRANCH_ID, user.branchId)
                .apply()

            // Audit log
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    userId = user.id,
                    userName = user.name,
                    businessId = user.businessId,
                    branchId = user.branchId,
                    action = AuditAction.LOGIN.name,
                    details = "ব্যবহারকারী লগইন করেছেন: ${user.name} (${user.role})"
                )
            )

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): UserEntity? = withContext(Dispatchers.IO) {
        val userId = prefs.getString(KEY_CURRENT_USER_ID, null) ?: return@withContext null
        db.userDao().getUserById(userId)
    }

    fun getCurrentBusinessId(): String? {
        return prefs.getString(KEY_CURRENT_BUSINESS_ID, null)
    }

    fun getCurrentBranchId(): String? {
        return prefs.getString(KEY_CURRENT_BRANCH_ID, null)
    }

    fun saveActiveBusinessAndBranch(businessId: String, branchId: String) {
        prefs.edit()
            .putString(KEY_CURRENT_BUSINESS_ID, businessId)
            .putString(KEY_CURRENT_BRANCH_ID, branchId)
            .apply()
    }

    suspend fun logout(user: UserEntity?) = withContext(Dispatchers.IO) {
        if (user != null) {
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    userId = user.id,
                    userName = user.name,
                    businessId = user.businessId,
                    branchId = user.branchId,
                    action = AuditAction.LOGOUT.name,
                    details = "লগআউট করেছেন: ${user.name}"
                )
            )
        }
        try {
            firebaseAuth.signOut()
        } catch (e: Exception) {
            // Ignored
        }
        prefs.edit().clear().apply()
    }

    suspend fun createInitialSuperAdmin(
        businessName: String,
        ownerName: String,
        phone: String,
        address: String,
        adminName: String,
        adminEmail: String,
        adminPassword: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val businessId = UUID.randomUUID().toString()
            val branchId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()

            // 1. Business
            val business = com.example.data.entity.BusinessEntity(
                id = businessId,
                name = businessName.trim(),
                ownerName = ownerName.trim(),
                phone = phone.trim(),
                address = address.trim(),
                isActive = true
            )
            db.businessDao().insertBusiness(business)

            // 2. Main Branch
            val branch = com.example.data.entity.BranchEntity(
                id = branchId,
                businessId = businessId,
                name = "প্রধান শাখা (Main)",
                address = address.trim(),
                phone = phone.trim(),
                status = "ACTIVE"
            )
            db.branchDao().insertBranch(branch)

            // 3. Super Admin User
            val allPermissions = com.example.data.model.ShopPermission.values().joinToString(",") { it.name }
            val superAdmin = UserEntity(
                id = userId,
                businessId = businessId,
                branchId = branchId,
                name = adminName.trim(),
                email = adminEmail.trim().lowercase(),
                phone = phone.trim(),
                role = UserRole.SUPER_ADMIN.name,
                status = UserStatus.APPROVED.name,
                permissionsCsv = allPermissions,
                passwordHash = hashPassword(adminPassword)
            )
            db.userDao().insertUser(superAdmin)

            // 4. Default Shop Settings
            val defaultSettings = com.example.data.entity.ShopSettingsEntity(
                id = "settings_$businessId",
                businessId = businessId
            )
            db.shopSettingsDao().insertOrUpdate(defaultSettings)

            // 5. Firebase create user attempt
            try {
                firebaseAuth.createUserWithEmailAndPassword(adminEmail.trim(), adminPassword).await()
            } catch (e: Exception) {
                // Offline fallback
            }

            // Save active session
            prefs.edit()
                .putString(KEY_CURRENT_USER_ID, userId)
                .putString(KEY_CURRENT_BUSINESS_ID, businessId)
                .putString(KEY_CURRENT_BRANCH_ID, branchId)
                .apply()

            db.auditLogDao().insertLog(
                AuditLogEntity(
                    userId = userId,
                    userName = adminName,
                    businessId = businessId,
                    branchId = branchId,
                    action = AuditAction.BUSINESS_CREATE.name,
                    details = "নতুন ব্যবসা ও সুপার অ্যাডমিন তৈরি: $businessName ($adminName)"
                )
            )

            Result.success(superAdmin)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
