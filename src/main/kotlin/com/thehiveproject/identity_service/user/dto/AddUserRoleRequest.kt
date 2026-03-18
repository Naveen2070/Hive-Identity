package com.thehiveproject.identity_service.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Request payload for adding a role to a user")
data class AddUserRoleRequest(
    @field:NotBlank(message = "Domain is required")
    @field:Schema(
        description = "Domain for the role",
        example = "events"
    )
    val domain: String,

    @field:NotBlank(message = "Role name is required")
    @field:Schema(
        description = "Name of the role",
        example = "ORGANIZER"
    )
    val roleName: String
)
