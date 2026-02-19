package com.thehiveproject.identity_service.auth.service

import com.thehiveproject.identity_service.user.entity.User

interface RefreshTokenService {
    fun createRefreshToken(userId: Long): String
    fun verifyAndGetUserId(token: String): User
    fun revokeTokensForUser(userId: Long)
}