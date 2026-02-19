package com.thehiveproject.identity_service.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.io.Serializable

@Schema(description = "Login request payload")
data class LoginRequest(

    @field:Schema(
        description = "User email address",
        example = "john.doe@example.com",
        required = true
    )
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,

    @field:Schema(
        description = "User password (minimum 8 characters)",
        example = "password123",
        required = true,
        minLength = 8
    )
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String
) : Serializable
