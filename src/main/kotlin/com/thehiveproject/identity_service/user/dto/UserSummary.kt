package com.thehiveproject.identity_service.user.dto

data class UserSummary(
    val id: Long,
    val fullName: String,
    val email: String
)