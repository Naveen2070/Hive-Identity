package com.thehiveproject.identity_service.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ResetPasswordRequest(
    @field:NotBlank(message = "Token is required")
    @param:Schema(
        description = "The UUID token received in the email link",
        example = "f81d4fae-7dec-41d0-a765-00a0c91e6bf6",
        required = true
    )
    val token: String,

    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters long")
    @param:Schema(
        example = "password123",
        description = "The new password for the account",
        required = true
    )
    val newPassword: String
)