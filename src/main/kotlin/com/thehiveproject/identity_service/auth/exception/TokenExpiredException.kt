package com.thehiveproject.identity_service.auth.exception

class TokenExpiredException(
    message: String = "JWT token has expired"
) : RuntimeException(message)