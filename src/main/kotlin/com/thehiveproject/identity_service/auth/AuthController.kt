package com.thehiveproject.identity_service.auth

import com.thehiveproject.identity_service.auth.dto.AuthResponse
import com.thehiveproject.identity_service.auth.dto.LoginRequest
import com.thehiveproject.identity_service.auth.dto.RegisterRequest
import com.thehiveproject.identity_service.auth.dto.TokenRefreshRequest
import com.thehiveproject.identity_service.common.exception.ApiErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication and registration APIs")
class AuthController(
    private val authService: AuthService
) {

    @Operation(
        summary = "Register new user",
        description = "Creates a new user account and returns access and refresh tokens"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "User successfully registered",
                content = [Content(schema = Schema(implementation = AuthResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Validation error or invalid role",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "User already exists",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            )
        ]
    )
    @PostMapping("/register")
    fun register(
        @Valid @RequestBody request: RegisterRequest
    ): ResponseEntity<AuthResponse> {
        val response = authService.registerUser(request)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Login user",
        description = "Authenticates user and returns access and refresh tokens"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Login successful",
                content = [Content(schema = Schema(implementation = AuthResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Validation error",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid email or password",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            )
        ]
    )
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest
    ): ResponseEntity<AuthResponse> {
        val response = authService.login(request)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Refresh access token",
        description = "Generates a new access token using a valid refresh token"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Access token successfully refreshed",
                content = [Content(schema = Schema(implementation = AuthResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Validation error",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired refresh token",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            )
        ]
    )
    @PostMapping("/refresh")
    fun refreshAccessToken(
        @Valid @RequestBody request: TokenRefreshRequest
    ): ResponseEntity<AuthResponse> {
        val response = authService.refreshToken(request)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Logout user",
        description = "Blacklists the access token and revokes the refresh token to log the user out"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "User successfully logged out",
                content = [Content()]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Validation error",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or missing access token",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            )
        ]
    )
    @PostMapping("/logout")
    fun logout(
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<Void> {
        authService.logout(token)
        return ResponseEntity.ok().build()
    }

}