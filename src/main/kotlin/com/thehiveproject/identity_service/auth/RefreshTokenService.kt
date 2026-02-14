package com.thehiveproject.identity_service.auth

import com.thehiveproject.identity_service.user.User

interface RefreshTokenService {
    fun createRefreshToken(userId: Long): String
    fun verifyAndGetUserId(token: String): User
    fun revokeTokensForUser(userId: Long)
}