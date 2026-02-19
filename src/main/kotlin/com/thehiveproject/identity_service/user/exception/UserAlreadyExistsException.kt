package com.thehiveproject.identity_service.user.exception

class UserAlreadyExistsException(message: String = "User already exists") : RuntimeException(message)