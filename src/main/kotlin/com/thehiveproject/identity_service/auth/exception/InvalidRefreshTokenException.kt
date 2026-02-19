package com.thehiveproject.identity_service.auth.exception

class InvalidRefreshTokenException(
    message: String = "Invalid refresh token"
) : RuntimeException(message)
