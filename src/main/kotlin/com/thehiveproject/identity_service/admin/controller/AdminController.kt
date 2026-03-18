package com.thehiveproject.identity_service.admin.controller

import com.thehiveproject.identity_service.auth.dto.CreateUserRequest
import com.thehiveproject.identity_service.common.dto.PaginatedResponse
import com.thehiveproject.identity_service.common.dto.toPaginatedResponse
import com.thehiveproject.identity_service.common.exception.ApiErrorResponse
import com.thehiveproject.identity_service.user.dto.AddUserRoleRequest
import com.thehiveproject.identity_service.user.dto.UpdateUserRolesRequest
import com.thehiveproject.identity_service.user.dto.UserDto
import com.thehiveproject.identity_service.user.mapper.toSanitized
import com.thehiveproject.identity_service.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Administrative user management APIs")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@SecurityRequirement(name = "bearerAuth")
class AdminController(
    private val userService: UserService
) {

    @Operation(summary = "List/Search users")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Users retrieved successfully", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = PaginatedResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "401", description = "Unauthorized", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "403", description = "Forbidden - Requires SUPER_ADMIN role", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "500", description = "Internal server error", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            )
        ]
    )
    @GetMapping("/users")
    fun getAllUsers(
        @PageableDefault(size = 20, sort = ["createdAt"]) pageable: Pageable,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<PaginatedResponse<UserDto>> {
        val users = userService.getAllUsers(pageable, search)
        return ResponseEntity.ok(users.toPaginatedResponse())
    }

    @Operation(summary = "Get user details by ID")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "User retrieved successfully", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UserDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "404", description = "User not found", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "401", description = "Unauthorized", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "403", description = "Forbidden", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "500", description = "Internal server error", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            )
        ]
    )
    @GetMapping("/users/{id}")
    fun getUserById(@PathVariable id: String): ResponseEntity<UserDto> {
        val user = userService.getUserById(id.toLong())
        return ResponseEntity.ok(user)
    }

    @Operation(
        summary = "Create internal user",
        description = "Create internal roles such as ADMIN, ORGANIZER manually"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201", description = "User created successfully", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UserDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "400", description = "Invalid request payload", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "401", description = "Unauthorized", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "403", description = "Forbidden", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "500", description = "Internal server error", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            )
        ]
    )
    @PostMapping("/users")
    fun createInternalUser(
        @Valid @RequestBody request: CreateUserRequest
    ): ResponseEntity<UserDto> {
        val user = userService.createInternalUser(request)
        return ResponseEntity.status(201).body(user.toSanitized())
    }

    @Operation(
        summary = "Ban or Unban user",
        description = "Toggle active status and revoke all active tokens"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "User status updated successfully", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UserDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "404", description = "User not found", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "401", description = "Unauthorized", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "403", description = "Forbidden", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "500", description = "Internal server error", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            )
        ]
    )
    @PatchMapping("/users/{id}/status")
    fun changeUserStatus(
        @PathVariable id: String,
        @RequestParam active: Boolean
    ): ResponseEntity<UserDto> {
        val user = userService.changeUserStatus(id.toLong(), active)
        return ResponseEntity.ok(user)
    }

    @Operation(
        summary = "Hard delete user",
        description = "Permanently deletes user from database (irreversible)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "User deleted successfully"),
            ApiResponse(
                responseCode = "404", description = "User not found", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "401", description = "Unauthorized", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "403", description = "Forbidden", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "500", description = "Internal server error", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            )
        ]
    )
    @DeleteMapping("/users/{id}/hard")
    fun hardDeleteUser(@PathVariable id: Long): ResponseEntity<Void> {
        userService.hardDeleteUser(id)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Add role to user", description = "Assign a new role to a user for a specific domain")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Role added successfully", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UserDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "404", description = "User or Role not found", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "400", description = "Invalid request payload", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            )
        ]
    )
    @PostMapping("/users/{id}/roles")
    fun addUserRole(
        @PathVariable id: Long,
        @Valid @RequestBody request: AddUserRoleRequest
    ): ResponseEntity<UserDto> {
        val user = userService.addUserRole(id, request.domain, request.roleName)
        return ResponseEntity.ok(user)
    }

    @Operation(summary = "Update user roles", description = "Update or assign multiple roles to a user across domains")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Roles updated successfully", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UserDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "404", description = "User or Role not found", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            )
        ]
    )
    @PutMapping("/users/{id}/roles")
    fun updateUserRoles(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateUserRolesRequest
    ): ResponseEntity<UserDto> {
        val user = userService.updateUserRoles(id, request.domainRoles)
        return ResponseEntity.ok(user)
    }

    @Operation(summary = "Remove role from user", description = "Remove a specific role from a user in a domain")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Role removed successfully", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UserDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "404", description = "User or Role not found", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            )
        ]
    )
    @DeleteMapping("/users/{id}/roles/{domain}/{roleName}")
    fun removeUserRole(
        @PathVariable id: Long,
        @PathVariable domain: String,
        @PathVariable roleName: String
    ): ResponseEntity<UserDto> {
        val user = userService.removeUserRole(id, domain, roleName)
        return ResponseEntity.ok(user)
    }
}