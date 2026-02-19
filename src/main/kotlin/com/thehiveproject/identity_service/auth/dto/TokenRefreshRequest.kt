package com.thehiveproject.identity_service.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class TokenRefreshRequest(
    @field:Schema(
        description = "Refresh token to be used for generating new access token",
        example = "f81d4fae-7dec-41d0-a765-00a0c91e6bf6"
    )
    @field:NotBlank(message = "Refresh token cannot be blank")
    val refreshToken: String
)