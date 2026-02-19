package com.thehiveproject.identity_service.user.exception

class UserAlreadyDeletedException(message: String = "User is already deleted") : RuntimeException(message)