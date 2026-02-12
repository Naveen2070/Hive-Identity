package com.thehiveproject.identity_service.auth

import com.thehiveproject.identity_service.auth.dto.AuthResponse
import com.thehiveproject.identity_service.auth.dto.LoginRequest
import com.thehiveproject.identity_service.auth.dto.RegisterRequest

interface AuthService {
    fun registerUser(user: RegisterRequest): AuthResponse
    fun login(loginRequest: LoginRequest): AuthResponse
}