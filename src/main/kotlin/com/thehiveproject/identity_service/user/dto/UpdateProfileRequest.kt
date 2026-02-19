package com.thehiveproject.identity_service.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "Request payload for updating user profile")
data class UpdateProfileRequest(
    @field:Size(min = 2, max = 100)
    @field:Schema(
        description = "Full name of the user",
        example = "John Doe"
    )
    val fullName: String? = null,

    )