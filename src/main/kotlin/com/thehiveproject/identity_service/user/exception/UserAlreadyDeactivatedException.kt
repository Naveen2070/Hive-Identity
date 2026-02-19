package com.thehiveproject.identity_service.user.exception

class UserAlreadyDeactivatedException(message: String = "User is already deactivated") : RuntimeException(message)