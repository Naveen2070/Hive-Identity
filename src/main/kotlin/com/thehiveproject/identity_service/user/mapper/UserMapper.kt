package com.thehiveproject.identity_service.user.mapper


import com.thehiveproject.identity_service.common.utils.sanitizeForHtml
import com.thehiveproject.identity_service.user.dto.UserDto
import com.thehiveproject.identity_service.user.entity.User
import com.thehiveproject.identity_service.user.entity.UserRole

object UserMapper {

    fun User.toDto(): UserDto {
        return UserDto(
            createdBy = this.createdBy,
            updatedBy = this.updatedBy,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            version = this.version,
            active = this.isEnabled(),
            deleted = this.isDeleted(),
            deletedAt = this.wasDeletedAt(),
            id = this.id.toString(),
            email = this.email,
            fullName = this.fullName,
            domainAccess = this.domainAccess.toSet(),
            roles = this.roles.map { it.toDto() }.toSet()
        )
    }


    private fun UserRole.toDto(): UserDto.UserRoleDto {
        return UserDto.UserRoleDto(
            createdBy = this.createdBy,
            updatedBy = this.updatedBy,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            version = this.version,
            active = this.isEnabled(),
            deleted = this.isDeleted(),
            deletedAt = this.wasDeletedAt(),
            id = this.id.toString(),
            roleId = this.role.id,
            roleName = this.role.name
        )
    }
}

fun UserDto.toSanitized(): UserDto {
    return this.copy(
        email = sanitizeForHtml(this.email),
        fullName = sanitizeForHtml(this.fullName),
        roles = this.roles.map { it.toSanitized() }.toMutableSet()
    )
}

fun UserDto.UserRoleDto.toSanitized(): UserDto.UserRoleDto {
    return this.copy(
        roleName = sanitizeForHtml(this.roleName)
    )
}