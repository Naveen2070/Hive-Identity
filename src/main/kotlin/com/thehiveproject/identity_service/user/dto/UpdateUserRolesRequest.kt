package com.thehiveproject.identity_service.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

@Schema(description = "Request payload for updating user roles across multiple domains")
data class UpdateUserRolesRequest(
    @field:NotEmpty(message = "Domain roles map cannot be empty")
    @field:Schema(
        description = "Map of domains to role names. This will replace or update roles for given domains.",
        example = "{\"events\": \"ORGANIZER\", \"movies\": \"ADMIN\"}"
    )
    val domainRoles: Map<String, String>
)
