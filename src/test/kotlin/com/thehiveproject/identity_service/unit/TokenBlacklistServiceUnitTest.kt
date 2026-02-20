package com.thehiveproject.identity_service.unit

import com.thehiveproject.identity_service.auth.service.TokenBlacklistServiceImpl
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TokenBlacklistServiceUnitTest {

    private lateinit var tokenBlacklistService: TokenBlacklistServiceImpl

    @BeforeEach
    fun setup() {
        // Instantiate a fresh service before each test to ensure a clean cache
        tokenBlacklistService = TokenBlacklistServiceImpl()
    }

    @Test
    fun `isBlacklisted should return false for a token that was never blacklisted`() {
        val token = "some.random.jwt.token"

        val result = tokenBlacklistService.isBlacklisted(token)

        assertFalse(result, "Token should not be blacklisted initially")
    }

    @Test
    fun `isBlacklisted should return true after token is added to blacklist`() {
        val token = "blacklisted.jwt.token"

        tokenBlacklistService.blacklistToken(token)
        val result = tokenBlacklistService.isBlacklisted(token)

        assertTrue(result, "Token should be blacklisted after being added")
    }

    @Test
    fun `blacklist should correctly track multiple independent tokens`() {
        val token1 = "token.number.one"
        val token2 = "token.number.two"
        val cleanToken = "token.never.blacklisted"

        // Blacklist two distinct tokens
        tokenBlacklistService.blacklistToken(token1)
        tokenBlacklistService.blacklistToken(token2)

        // Verify both are blacklisted
        assertTrue(tokenBlacklistService.isBlacklisted(token1))
        assertTrue(tokenBlacklistService.isBlacklisted(token2))

        // Verify a completely different token is still allowed
        assertFalse(tokenBlacklistService.isBlacklisted(cleanToken))
    }
}