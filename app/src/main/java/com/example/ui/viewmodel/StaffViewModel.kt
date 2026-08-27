package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.data.database.AppDatabase
import com.example.data.entity.AuditLogEntity
import com.example.data.entity.UserEntity
import com.example.data.model.AuditAction
import com.example.data.model.ShopPermission
import com.example.data.model.UserRole
import com.example.data.model.UserStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

class StaffViewModel(private val db: AppDatabase) : ViewModel() {

    fun getUsersByBusiness(businessId: String): Flow<List<UserEntity>> {
        return db.userDao().getUsersByBusiness(businessId)
    }

    fun getPendingUsers(businessId: String): Flow<List<UserEntity>> {
        return db.userDao().getPendingUsers(businessId)
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    suspend fun createStaffOrAdmin(
        name: String,
        email: String,
        phone: String,
        password: String,
        role: UserRole,
        businessId: String,
        branchId: String,
        assignedPermissions: List<ShopPermission>,
        creatorUser: UserEntity
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = db.userDao().getUserByEmail(email.trim().lowercase())
            if (existing != null) {
                return@withContext Result.failure(Exception("এই ইমেইল দিয়ে ইতিমধ্যে অ্যাকাউন্ট রয়েছে!"))
            }

            val newUser = UserEntity(
                id = UUID.randomUUID().toString(),
                businessId = businessId,
                branchId = branchId,
                name = name.trim(),
                email = email.trim().lowercase(),
                phone = phone.trim(),
                role = role.name,
                status = if (role == UserRole.STAFF) UserStatus.PENDING.name else UserStatus.APPROVED.name,
                permissionsCsv = assignedPermissions.joinToString(",") { it.name },
                passwordHash = hashPassword(password)
            )

            db.userDao().insertUser(newUser)

            db.auditLogDao().insertLog(
                AuditLogEntity(
                    userId = creatorUser.id,
                    userName = creatorUser.name,
                    businessId = businessId,
                    branchId = branchId,
                    action = AuditAction.USER_CREATE.name,
                    recordId = newUser.id,
                    details = "নতুন ব্যবহারকারী তৈরি: ${newUser.name} (${role.banglaTitle} - ${newUser.status})"
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approveStaff(
        user: UserEntity,
        adminUser: UserEntity
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.userDao().updateUserStatus(user.id, UserStatus.APPROVED.name)
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    userId = adminUser.id,
                    userName = adminUser.name,
                    businessId = user.businessId,
                    branchId = user.branchId,
                    action = AuditAction.USER_APPROVE.name,
                    recordId = user.id,
                    details = "স্টাফ অনুমোদন করা হয়েছে: ${user.name} (${user.email})"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectStaff(
        user: UserEntity,
        adminUser: UserEntity
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.userDao().updateUserStatus(user.id, UserStatus.REJECTED.name)
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    userId = adminUser.id,
                    userName = adminUser.name,
                    businessId = user.businessId,
                    branchId = user.branchId,
                    action = AuditAction.USER_REJECT.name,
                    recordId = user.id,
                    details = "স্টাফ আবেদন বাতিল করা হয়েছে: ${user.name}"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleUserActive(
        user: UserEntity,
        adminUser: UserEntity
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val newStatus = if (user.status == UserStatus.APPROVED.name) UserStatus.DISABLED.name else UserStatus.APPROVED.name
            db.userDao().updateUserStatus(user.id, newStatus)
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    userId = adminUser.id,
                    userName = adminUser.name,
                    businessId = user.businessId,
                    branchId = user.branchId,
                    action = AuditAction.USER_DISABLE.name,
                    recordId = user.id,
                    details = "ব্যবহারকারীর স্ট্যাটাস পরিবর্তন: ${user.name} -> $newStatus"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePermissions(
        user: UserEntity,
        newPermissions: List<ShopPermission>,
        adminUser: UserEntity
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val csv = newPermissions.joinToString(",") { it.name }
            db.userDao().updateUserPermissions(user.id, csv)
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    userId = adminUser.id,
                    userName = adminUser.name,
                    businessId = user.businessId,
                    branchId = user.branchId,
                    action = AuditAction.PERMISSION_CHANGE.name,
                    recordId = user.id,
                    details = "অনুমতি পরিবর্তন: ${user.name}"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
