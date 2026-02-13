package com.thehiveproject.identity_service.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Request payload for changing user password")
data class ChangePasswordRequest(

    @field:NotBlank(message = "Old password must not be blank")
    @field:Schema(
        description = "Current password of the user",
        example = "CurrentPassword123!",
        format = "password",
        writeOnly = true
    )
    val oldPassword: String,

    @field:NotBlank(message = "New password must not be blank")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    @field:Schema(
        description = "New password to replace the current one (minimum 8 characters)",
        example = "NewSecurePassword123!",
        format = "password",
        writeOnly = true
    )
    val newPassword: String
)
