package com.thehiveproject.identity_service.user.dto

import java.io.Serializable
import java.time.Instant

data class UserDto(
    val createdBy: Long? = null,
    val updatedBy: Long? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val version: Long = 0,
    val active: Boolean = true,
    val deleted: Boolean = false,
    val deletedAt: Instant? = null,
    val id: String? = null,
    val email: String? = null,
    val fullName: String? = null,
    val domainAccess: Set<String> = setOf("events"),
    val roles: Set<UserRoleDto> = setOf()
) : Serializable {
    data class UserRoleDto(
        val createdBy: Long? = null,
        val updatedBy: Long? = null,
        val createdAt: Instant = Instant.now(),
        val updatedAt: Instant = Instant.now(),
        val version: Long = 0,
        val active: Boolean = true,
        val deleted: Boolean = false,
        val deletedAt: Instant? = null,
        val id: String? = null,
        val roleId: Int? = null,
        val roleName: String? = null
    ) : Serializable
}