package com.example.service

import com.example.data.entity.UserEntity
import com.example.data.model.ShopPermission
import com.example.data.model.UserRole
import com.example.data.model.UserStatus

object AuthorizationService {

    fun isUserApproved(user: UserEntity?): Boolean {
        if (user == null) return false
        return user.status == UserStatus.APPROVED.name
    }

    fun hasPermission(user: UserEntity?, permission: ShopPermission): Boolean {
        if (user == null) return false
        
        // Block any non-approved or disabled users
        if (user.status != UserStatus.APPROVED.name) return false

        // SUPER_ADMIN has unrestricted authorization
        if (user.role == UserRole.SUPER_ADMIN.name) return true

        // ADMIN has almost all business and branch level permissions
        if (user.role == UserRole.ADMIN.name) {
            return true
        }

        // STAFF: Check assigned permissions CSV
        val userPermissions = user.permissionsCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        return userPermissions.contains(permission.name)
    }

    fun canManageUsers(user: UserEntity?): Boolean {
        if (user == null || user.status != UserStatus.APPROVED.name) return false
        return user.role == UserRole.SUPER_ADMIN.name || user.role == UserRole.ADMIN.name
    }

    fun canApproveStaff(user: UserEntity?): Boolean {
        if (user == null || user.status != UserStatus.APPROVED.name) return false
        return user.role == UserRole.SUPER_ADMIN.name || user.role == UserRole.ADMIN.name
    }

    fun canSwitchBusiness(user: UserEntity?): Boolean {
        if (user == null || user.status != UserStatus.APPROVED.name) return false
        return user.role == UserRole.SUPER_ADMIN.name
    }

    fun canManageBranches(user: UserEntity?): Boolean {
        if (user == null || user.status != UserStatus.APPROVED.name) return false
        return user.role == UserRole.SUPER_ADMIN.name || user.role == UserRole.ADMIN.name
    }

    fun getDefaultStaffPermissions(): List<ShopPermission> {
        return listOf(
            ShopPermission.VIEW_STOCK,
            ShopPermission.CREATE_SALE,
            ShopPermission.VIEW_SALES,
            ShopPermission.VIEW_CUSTOMERS,
            ShopPermission.EDIT_CUSTOMERS,
            ShopPermission.RECORD_PAYMENT,
            ShopPermission.VIEW_DUE,
            ShopPermission.SEND_SMS,
            ShopPermission.SEND_WHATSAPP
        )
    }
}
