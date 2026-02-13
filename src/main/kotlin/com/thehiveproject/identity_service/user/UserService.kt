package com.thehiveproject.identity_service.user

import com.thehiveproject.identity_service.user.dto.ChangePasswordRequest
import com.thehiveproject.identity_service.user.dto.UpdateProfileRequest
import com.thehiveproject.identity_service.user.dto.UserResponse

interface UserService {
    fun getUserProfile(email: String): UserResponse
    fun updateProfile(email: String, request: UpdateProfileRequest): UserResponse
    fun changePassword(email: String, request: ChangePasswordRequest)
    fun deactivateAccount(email: String)
    fun deleteAccount(email: String)
}