package com.thehiveproject.identity_service.auth.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Authentication response containing JWT token")
data class AuthResponse(

    @field:Schema(
        description = "JWT access token",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    val token: String,

    @field:Schema(
        description = "Refresh token",
        example = "f81d4fae-7dec-41d0-a765-00a0c91e6bf6"
    )
    val refreshToken: String,

    @field:Schema(
        description = "Authenticated user email",
        example = "john.doe@example.com"
    )
    val email: String,
)
