package com.thehiveproject.identity_service.auth.exception

class InvalidPasswordException(
    message: String = "Invalid password"
) : RuntimeException(message)