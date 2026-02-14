package com.thehiveproject.identity_service.auth

import com.thehiveproject.identity_service.auth.dto.AuthResponse
import com.thehiveproject.identity_service.auth.dto.LoginRequest
import com.thehiveproject.identity_service.auth.dto.RegisterRequest
import com.thehiveproject.identity_service.auth.dto.TokenRefreshRequest

interface AuthService {
    fun registerUser(user: RegisterRequest): AuthResponse
    fun login(loginRequest: LoginRequest): AuthResponse
    fun refreshToken(request: TokenRefreshRequest): AuthResponse
    fun initiatePasswordReset(email: String)
    fun completePasswordReset(tokenString: String, newPassword: String)
    fun logout(token: String)
}