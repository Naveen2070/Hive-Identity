package com.thehiveproject.identity_service.auth.dto

data class AuthResponse(
    val token: String,
    val email: String,
)