package com.thehiveproject.identity_service.auth.exception

class InvalidCredentialsException(
    message: String = "Invalid email or password"
) : RuntimeException(message)
