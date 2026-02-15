package com.thehiveproject.identity_service.user.dto

import com.thehiveproject.identity_service.user.entity.User
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "User profile response")
data class UserResponse(

    @field:Schema(
        description = "Unique identifier of the user",
        example = "281088190180233216"
    )
    val id: String,

    @field:Schema(
        description = "Full name of the user",
        example = "John Doe"
    )
    val fullName: String,

    @field:Schema(
        description = "Email address of the user",
        example = "john.doe@example.com"
    )
    val email: String,

    @field:Schema(
        description = "List of domain identifiers the user has access to",
        example = "[\"domain-a\", \"domain-b\"]"
    )
    val domainAccess: Set<String>,

    @field:Schema(
        description = "Roles assigned to the user",
        example = "[\"ADMIN\", \"USER\"]"
    )
    val roles: List<String>,

    @field:Schema(
        description = "Timestamp when the user account was created",
        example = "2024-01-15T10:15:30Z"
    )
    val createdAt: Instant,

    @field:Schema(
        description = "Indicates whether the user account is active",
        example = "true"
    )
    val isActive: Boolean
) {
    companion object {
        fun fromEntity(user: User): UserResponse {
            return UserResponse(
                id = user.id.toString(),
                fullName = user.fullName,
                email = user.email,
                domainAccess = user.domainAccess,
                roles = user.roles.map { it.role.name },
                createdAt = user.createdAt,
                isActive = user.isEnabled()
            )
        }
    }
}