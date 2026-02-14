package com.thehiveproject.identity_service.user

import com.thehiveproject.identity_service.auth.security.CustomUserDetails
import com.thehiveproject.identity_service.common.exception.ApiErrorResponse
import com.thehiveproject.identity_service.user.dto.ChangePasswordRequest
import com.thehiveproject.identity_service.user.dto.UpdateProfileRequest
import com.thehiveproject.identity_service.user.dto.UserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User profile and management APIs")
class UserController(
    private val userService: UserService
) {

    @Operation(
        summary = "Get current authenticated user",
        description = "Returns the profile information of the currently authenticated user",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "User profile retrieved successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UserResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - JWT token missing or invalid",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            )
        ]
    )
    @GetMapping("/me")
    fun getCurrentUser(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<UserResponse> {
        val response = userService.getUserProfile(userDetails.username)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Update current authenticated user profile",
        description = "Updates the profile information of the currently authenticated user",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "User profile updated successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UserResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request payload",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - JWT token missing or invalid",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            )
        ]
    )
    @PatchMapping("/me")
    fun updateProfile(
        @AuthenticationPrincipal userDetails: UserDetails,
        @Valid
        @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<UserResponse> {
        val response = userService.updateProfile(userDetails.username, request)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Change current authenticated user password",
        description = "Changes the password of the currently authenticated user",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Password changed successfully"
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request payload or validation failed",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - JWT token missing or invalid",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Old password is incorrect",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            )
        ]
    )
    @PostMapping("/change-password")
    fun changePassword(
        @AuthenticationPrincipal userDetails: UserDetails,
        @Valid
        @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<Void> {
        userService.changePassword(userDetails.username, request)
        return ResponseEntity.ok().build()
    }

    @Operation(
        summary = "Deactivate current authenticated user account",
        description = "Deactivates the currently authenticated user's account",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "Account deactivated successfully"
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - JWT token missing or invalid",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            )
        ]
    )

    @DeleteMapping("/deactivate/me")
    fun deactivateAccount(
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<Void> {
        userService.deactivateAccount(userDetails.username)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Delete current authenticated user account",
        description = "Soft delete the currently authenticated user's account",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "Account deleted successfully"
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - JWT token missing or invalid",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiErrorResponse::class)
                )]
            )
        ]
    )
    @DeleteMapping("/delete/me")
    fun deleteAccount(
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<Void> {
        userService.deleteAccount(userDetails.username)
        return ResponseEntity.noContent().build()
    }

}
