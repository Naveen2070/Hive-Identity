package com.thehiveproject.identity_service.auth

interface TokenBlacklistService {
    fun blacklistToken(token: String)
    fun isBlacklisted(token: String): Boolean
}