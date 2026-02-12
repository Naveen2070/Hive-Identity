package com.thehiveproject.identity_service.user.exception

class RoleNotFoundException(name: String, message: String = "Role with following name not found") :
    RuntimeException("$message=$name")