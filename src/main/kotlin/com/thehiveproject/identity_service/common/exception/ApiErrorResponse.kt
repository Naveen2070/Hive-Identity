package com.thehiveproject.identity_service.common.exception

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Standard API error response")
data class ApiErrorResponse(

    @field:Schema(
        description = "Timestamp when the error occurred (ISO-8601 format)",
        type = "string",
        format = "date-time",
        example = "2026-02-12T14:32:10"
    )
    val timestamp: LocalDateTime = LocalDateTime.now(),

    @field:Schema(
        description = "HTTP status code",
        example = "400"
    )
    val status: Int,

    @field:Schema(
        description = "HTTP error reason",
        example = "Bad Request"
    )
    val error: String,

    @field:Schema(
        description = "Detailed error message",
        example = "Email is required"
    )
    val message: String,

    @field:Schema(
        description = "API endpoint path where the error occurred",
        example = "/auth/login"
    )
    val path: String
)
