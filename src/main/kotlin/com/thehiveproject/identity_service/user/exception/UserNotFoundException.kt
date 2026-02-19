package com.thehiveproject.identity_service.user.exception

class UserNotFoundException(uniqueId: String, message: String = "User with following details not found") :
    RuntimeException("$message=$uniqueId")
