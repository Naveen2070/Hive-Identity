package com.thehiveproject.identity_service.unit

import com.thehiveproject.identity_service.auth.entity.RefreshToken
import com.thehiveproject.identity_service.auth.exception.InvalidRefreshTokenException
import com.thehiveproject.identity_service.auth.exception.TokenExpiredException
import com.thehiveproject.identity_service.auth.repository.RefreshTokenRepository
import com.thehiveproject.identity_service.auth.service.RefreshTokenServiceImpl
import com.thehiveproject.identity_service.config.JwtProperties
import com.thehiveproject.identity_service.user.entity.User
import com.thehiveproject.identity_service.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import java.time.Instant
import java.util.*

@ExtendWith(MockitoExtension::class)
class RefreshTokenServiceUnitTest {

    @Mock
    lateinit var refreshTokenRepository: RefreshTokenRepository

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var jwtProperties: JwtProperties

    @InjectMocks
    lateinit var refreshTokenService: RefreshTokenServiceImpl

    // --- Helpers ---
    private val userId = 1L
    private val dummyTokenString = UUID.randomUUID().toString()

    private fun createDummyUser(): User {
        val user = User(
            email = "user@test.com",
            passwordHash = "hash",
            fullName = "Test User",
            domainAccess = mutableSetOf("ALL")
        )
        user.id = userId
        return user
    }

    // ==========================================
    // 1. CREATE REFRESH TOKEN TESTS
    // ==========================================

    @Test
    fun `createRefreshToken should revoke old tokens and return a new token`() {
        val user = createDummyUser()
        val mockJwtExpirationMs = 3600000L // 1 hour

        `when`(jwtProperties.expirationMs).thenReturn(mockJwtExpirationMs)
        `when`(userRepository.getReferenceById(userId)).thenReturn(user)

        // When save is called, return the entity that was passed in
        `when`(refreshTokenRepository.save(any<RefreshToken>())).thenAnswer {
            it.arguments[0] as RefreshToken
        }

        val generatedToken = refreshTokenService.createRefreshToken(userId)

        // 1. Assert old tokens were revoked
        verify(refreshTokenRepository).deleteByUserId(userId)

        // 2. Assert a new token was saved and returned
        assertNotNull(generatedToken)
        verify(refreshTokenRepository).save(check { savedEntity ->
            assertEquals(user, savedEntity.user)
            assertEquals(generatedToken, savedEntity.token)
            // Ensure expiry date is in the future
            assert(savedEntity.expiryDate.isAfter(Instant.now()))
        })
    }

    // ==========================================
    // 2. VERIFY TOKEN TESTS
    // ==========================================

    @Test
    fun `verifyAndGetUserId should return User when token is valid and unexpired`() {
        val user = createDummyUser()
        val validTokenEntity = RefreshToken(
            user = user,
            token = dummyTokenString,
            expiryDate = Instant.now().plusSeconds(3600) // Expires in 1 hour
        )

        `when`(refreshTokenRepository.findByToken(dummyTokenString)).thenReturn(Optional.of(validTokenEntity))

        val extractedUser = refreshTokenService.verifyAndGetUserId(dummyTokenString)

        assertEquals(user, extractedUser)
    }

    @Test
    fun `verifyAndGetUserId should throw InvalidRefreshTokenException when token is not found`() {
        `when`(refreshTokenRepository.findByToken("unknown-token")).thenReturn(Optional.empty())

        assertThrows<InvalidRefreshTokenException> {
            refreshTokenService.verifyAndGetUserId("unknown-token")
        }
    }

    @Test
    fun `verifyAndGetUserId should throw TokenExpiredException and delete token when expired`() {
        val user = createDummyUser()
        val expiredTokenEntity = RefreshToken(
            user = user,
            token = dummyTokenString,
            expiryDate = Instant.now().minusSeconds(3600) // Expired 1 hour ago
        )

        `when`(refreshTokenRepository.findByToken(dummyTokenString)).thenReturn(Optional.of(expiredTokenEntity))

        assertThrows<TokenExpiredException> {
            refreshTokenService.verifyAndGetUserId(dummyTokenString)
        }

        // Crucial check: The service should proactively clean up the expired token
        verify(refreshTokenRepository).delete(expiredTokenEntity)
    }

    // ==========================================
    // 3. REVOKE TOKENS TESTS
    // ==========================================

    @Test
    fun `revokeTokensForUser should delete tokens by userId`() {
        refreshTokenService.revokeTokensForUser(userId)

        verify(refreshTokenRepository).deleteByUserId(userId)
    }
}