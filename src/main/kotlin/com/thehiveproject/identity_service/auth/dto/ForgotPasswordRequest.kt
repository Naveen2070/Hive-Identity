package com.thehiveproject.identity_service.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class ForgotPasswordRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    @param:Schema(
        example = "john.doe@example.com",
        description = "The email address of the account to recover",
        required = true
    )
    val email: String
)