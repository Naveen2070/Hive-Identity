package com.thehiveproject.identity_service.internal.controller

import com.thehiveproject.identity_service.common.exception.ApiErrorResponse
import com.thehiveproject.identity_service.user.dto.UserSummary
import com.thehiveproject.identity_service.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/api/internal/users")
class InternalUserController(
    private val userService: UserService
) {

    @Operation(
        summary = "Get user summary by ID",
        description = "Returns basic user information (summary) by user ID",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "User retrieved successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UserSummary::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid user ID supplied",
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
    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: Long): ResponseEntity<UserSummary> {
        val user = userService.getUserSummaryById(id)
        return ResponseEntity.ok(UserSummary(user.id, user.fullName, user.email))
    }


    @Operation(
        summary = "Resolve multiple users by IDs",
        description = "Returns a list of user summaries for the given list of user IDs",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Users retrieved successfully",
                content = [Content(
                    mediaType = "application/json",
                    array = ArraySchema(
                        schema = Schema(implementation = UserSummary::class)
                    )
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
                responseCode = "404",
                description = "One or more users not found",
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
    @PostMapping("/batch")
    fun resolveUsers(@RequestBody ids: List<Long>): ResponseEntity<List<UserSummary>> {
        val usersList = userService.findBatchUserSummary(ids)
        return ResponseEntity.ok(usersList)
    }

}
