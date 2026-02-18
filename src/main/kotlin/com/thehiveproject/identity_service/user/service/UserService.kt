package com.thehiveproject.identity_service.user.service

import com.thehiveproject.identity_service.auth.dto.CreateUserRequest
import com.thehiveproject.identity_service.user.dto.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface UserService {
    fun createInternalUser(request: CreateUserRequest): UserDto
    fun getUserProfile(email: String): UserResponse
    fun getAllUsers(pageable: Pageable, search: String?): Page<UserDto>
    fun getUserById(id: Long): UserDto
    fun updateProfile(email: String, request: UpdateProfileRequest): UserResponse
    fun changeUserStatus(id: Long, active: Boolean): UserDto
    fun changePassword(email: String, request: ChangePasswordRequest)
    fun deactivateAccount(email: String)
    fun deleteAccount(email: String)
    fun hardDeleteUser(id: Long)
    fun getUserSummaryById(id: Long): UserSummary
    fun findBatchUserSummary(ids: List<Long>): List<UserSummary>
}