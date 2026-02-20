package com.thehiveproject.identity_service.unit

import com.thehiveproject.identity_service.auth.service.JwtService
import com.thehiveproject.identity_service.config.JwtProperties
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.security.SignatureException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.lenient
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@ExtendWith(MockitoExtension::class)
class JwtServiceUnitTest {

    private lateinit var jwtService: JwtService

    // A standard 256-bit base64 encoded secret key required by HMAC-SHA256
    private val testSecret = "4qhq8L6M0b6g6Q8j7W5E4X8Z2c4V6B9A1D3F5H7J9K0="
    private val testExpirationMs = 3600000L // 1 hour

    @BeforeEach
    fun setup() {
        // Mock the properties so we don't rely on Spring Boot's context
        val properties = mock(JwtProperties::class.java)
        `when`(properties.secret).thenReturn(testSecret)
        `when`(properties.expirationMs).thenReturn(testExpirationMs)

        jwtService = JwtService(properties)
    }

    // Helper to quickly generate a mocked UserDetails object
    private fun mockUser(username: String): UserDetails {
        val userDetails = mock(UserDetails::class.java)
        lenient().`when`(userDetails.username).thenReturn(username)
        lenient().`when`(userDetails.authorities).thenReturn(listOf(SimpleGrantedAuthority("ROLE_USER")))
        return userDetails
    }

    // ==========================================
    // 1. GENERATION TESTS
    // ==========================================

    @Test
    fun `generateToken should return a valid JWT string format`() {
        val user = mockUser("user@test.com")
        val token = jwtService.generateToken(user)

        assertNotNull(token)
        // A valid JWT always has 3 parts separated by dots: Header.Payload.Signature
        assertEquals(3, token.split(".").size)
    }

    // ==========================================
    // 2. EXTRACTION TESTS
    // ==========================================

    @Test
    fun `extractUsername should return correct subject from token`() {
        val user = mockUser("user@test.com")
        val token = jwtService.generateToken(user)

        val extractedUsername = jwtService.extractUsername(token)

        assertEquals("user@test.com", extractedUsername)
    }

    @Test
    fun `extractId should return correct ID from extra claims`() {
        val user = mockUser("user@test.com")
        val extraClaims = mapOf("id" to 999L)
        val token = jwtService.generateToken(extraClaims, user)

        val extractedId = jwtService.extractId(token)

        assertEquals(999L, extractedId)
    }

    // ==========================================
    // 3. VALIDATION & EDGE CASE TESTS
    // ==========================================

    @Test
    fun `isTokenValid should return true for valid user and unexpired token`() {
        val user = mockUser("user@test.com")
        val token = jwtService.generateToken(user)

        assertTrue(jwtService.isTokenValid(token, user))
    }

    @Test
    fun `isTokenValid should return false if username does not match`() {
        val user1 = mockUser("user1@test.com")
        val user2 = mockUser("user2@test.com")
        val token = jwtService.generateToken(user1)

        assertFalse(jwtService.isTokenValid(token, user2))
    }

    @Test
    fun `parsing an expired token should throw ExpiredJwtException`() {
        // Setup a separate JwtService with a negative expiration so tokens expire instantly
        val expiredProperties = mock(JwtProperties::class.java)
        `when`(expiredProperties.secret).thenReturn(testSecret)
        `when`(expiredProperties.expirationMs).thenReturn(-1000L) // Expires 1 second in the past

        val expiredJwtService = JwtService(expiredProperties)

        val user = mockUser("user@test.com")
        val token = expiredJwtService.generateToken(user)

        // The JJWT library throws an exception immediately upon parsing an expired token
        assertThrows<ExpiredJwtException> {
            expiredJwtService.isTokenValid(token, user)
        }
    }

    @Test
    fun `tampered token should throw SignatureException`() {
        val user = mockUser("user@test.com")
        val token = jwtService.generateToken(user)

        // Simulating a hacker modifying the signature at the end of the token
        val tamperedToken = token.dropLast(2) + "ab"

        assertThrows<SignatureException> {
            jwtService.isTokenValid(tamperedToken, user)
        }
    }
}