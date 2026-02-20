package com.thehiveproject.identity_service.unit

import com.thehiveproject.identity_service.auth.dto.LoginRequest
import com.thehiveproject.identity_service.auth.dto.RegisterRequest
import com.thehiveproject.identity_service.auth.dto.TokenRefreshRequest
import com.thehiveproject.identity_service.auth.entity.PasswordResetToken
import com.thehiveproject.identity_service.auth.event.ForgotPasswordEvent
import com.thehiveproject.identity_service.auth.exception.TokenExpiredException
import com.thehiveproject.identity_service.auth.repository.PasswordResetTokenRepository
import com.thehiveproject.identity_service.auth.security.CustomUserDetails
import com.thehiveproject.identity_service.auth.service.AuthServiceImpl
import com.thehiveproject.identity_service.auth.service.JwtService
import com.thehiveproject.identity_service.auth.service.RefreshTokenService
import com.thehiveproject.identity_service.auth.service.TokenBlacklistService
import com.thehiveproject.identity_service.user.entity.Role
import com.thehiveproject.identity_service.user.entity.User
import com.thehiveproject.identity_service.user.entity.UserRole
import com.thehiveproject.identity_service.user.exception.RoleNotFoundException
import com.thehiveproject.identity_service.user.exception.UserAlreadyExistsException
import com.thehiveproject.identity_service.user.repository.RoleRepository
import com.thehiveproject.identity_service.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.*

@ExtendWith(MockitoExtension::class)
class AuthServiceUnitTest {

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var roleRepository: RoleRepository

    @Mock
    lateinit var authenticationManager: AuthenticationManager

    @Mock
    lateinit var jwtService: JwtService

    @Mock
    lateinit var refreshTokenService: RefreshTokenService

    @Mock
    lateinit var passwordEncoder: PasswordEncoder

    @Mock
    lateinit var tokenBlacklistService: TokenBlacklistService

    @Mock
    lateinit var passwordResetTokenRepository: PasswordResetTokenRepository

    @Mock
    lateinit var eventPublisher: ApplicationEventPublisher

    @InjectMocks
    lateinit var authService: AuthServiceImpl

    // --- Helpers ---
    private val defaultEmail = "test@test.com"
    private val defaultPassword = "password123"
    private val encodedPassword = "encodedPassword123"

    private fun createDummyUser(): User {
        val user = User(
            email = defaultEmail,
            passwordHash = encodedPassword,
            fullName = "Test User",
            domainAccess = mutableSetOf("ALL")
        )
        user.id = 1L

        val role = Role(1, "Standard user")

        val userRole = UserRole(1L, user, role)
        user.roles.add(userRole)

        return user
    }

    // ==========================================
    // 1. REGISTER TESTS
    // ==========================================

    @Test
    fun `registerUser should succeed for valid USER role`() {
        val request = RegisterRequest(defaultEmail, defaultPassword, "Test User", mutableSetOf("ALL"), "USER")
        val role = Role(1, "Standard User")
        val savedUser = createDummyUser()

        `when`(userRepository.findByEmail(request.email)).thenReturn(Optional.empty())
        `when`(roleRepository.findByName(request.role)).thenReturn(Optional.of(role))
        `when`(passwordEncoder.encode(request.password)).thenReturn(encodedPassword)
        `when`(userRepository.save(any())).thenReturn(savedUser)
        `when`(jwtService.generateToken(any(), any())).thenReturn("access-token")
        `when`(refreshTokenService.createRefreshToken(savedUser.id!!)).thenReturn("refresh-token")

        val response = authService.registerUser(request)

        assertEquals("access-token", response.token)
        assertEquals("refresh-token", response.refreshToken)
        assertEquals(defaultEmail, response.email)
        verify(userRepository).save(any())
    }

    @Test
    fun `registerUser should throw exception if user already exists`() {
        val request = RegisterRequest("Test", defaultEmail, defaultPassword, mutableSetOf("ALL"), "USER")
        `when`(userRepository.findByEmail(defaultEmail)).thenReturn(Optional.of(createDummyUser()))

        assertThrows<UserAlreadyExistsException> {
            authService.registerUser(request)
        }
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `registerUser should throw exception if role is not allowed`() {
        val request = RegisterRequest("Test", defaultEmail, defaultPassword, mutableSetOf("ALL"), "ADMIN")
        `when`(userRepository.findByEmail(defaultEmail)).thenReturn(Optional.empty())

        val exception = assertThrows<IllegalArgumentException> {
            authService.registerUser(request)
        }
        assertTrue(exception.message!!.contains("Invalid role"))
    }

    @Test
    fun `registerUser should throw exception if role does not exist in DB`() {
        val request = RegisterRequest("Test", defaultEmail, defaultPassword, mutableSetOf("ALL"), "USER")
        `when`(userRepository.findByEmail(defaultEmail)).thenReturn(Optional.empty())
        `when`(roleRepository.findByName("USER")).thenReturn(Optional.empty())

        assertThrows<RoleNotFoundException> {
            authService.registerUser(request)
        }
    }

    // ==========================================
    // 2. LOGIN TESTS
    // ==========================================

    @Test
    fun `login should return tokens on successful authentication`() {
        val request = LoginRequest(defaultEmail, defaultPassword)
        val user = createDummyUser()

        val userDetails = CustomUserDetails(
            id = user.id!!,
            username = user.email,
            password = user.passwordHash,
            enabled = true,
            accountNonExpired = true,
            credentialsNonExpired = true,
            accountNonLocked = true,
            authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        )

        val authObj = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)

        `when`(userRepository.findByEmail(request.email)).thenReturn(Optional.of(user))
        `when`(authenticationManager.authenticate(any())).thenReturn(authObj)
        `when`(jwtService.generateToken(any(), eq(userDetails))).thenReturn("access-token")
        `when`(refreshTokenService.createRefreshToken(user.id!!)).thenReturn("refresh-token")

        val response = authService.login(request)

        assertEquals("access-token", response.token)
        assertEquals("refresh-token", response.refreshToken)
    }

    @Test
    fun `login should throw UsernameNotFoundException if email not found`() {
        val request = LoginRequest("wrong@test.com", defaultPassword)
        `when`(userRepository.findByEmail(request.email)).thenReturn(Optional.empty())

        assertThrows<UsernameNotFoundException> {
            authService.login(request)
        }
    }

    @Test
    fun `login should throw BadCredentialsException on wrong password`() {
        val request = LoginRequest(defaultEmail, "wrongPass")
        val user = createDummyUser()

        `when`(userRepository.findByEmail(request.email)).thenReturn(Optional.of(user))
        `when`(authenticationManager.authenticate(any())).thenThrow(BadCredentialsException("Bad credentials"))

        assertThrows<BadCredentialsException> {
            authService.login(request)
        }
    }

    // ==========================================
    // 3. REFRESH TOKEN TESTS
    // ==========================================

    @Test
    fun `refreshToken should return new access token`() {
        val request = TokenRefreshRequest("valid-refresh-token")
        val user = createDummyUser()

        `when`(refreshTokenService.verifyAndGetUserId(request.refreshToken)).thenReturn(user)
        `when`(jwtService.generateToken(any(), any())).thenReturn("new-access-token")

        val response = authService.refreshToken(request)

        assertEquals("new-access-token", response.token)
        assertEquals("valid-refresh-token", response.refreshToken)
    }

    // ==========================================
    // 4. FORGOT PASSWORD TESTS
    // ==========================================

    @Test
    fun `initiatePasswordReset should silently return if user not found`() {
        `when`(userRepository.findActiveUser(defaultEmail)).thenReturn(Optional.empty())

        // Should not throw any exception, should not interact with token repos
        authService.initiatePasswordReset(defaultEmail)

        verify(passwordResetTokenRepository, never()).deleteByUser(any())
        verify(passwordResetTokenRepository, never()).save(any())
        verify(eventPublisher, never()).publishEvent(any())
    }

    @Test
    fun `initiatePasswordReset should create token and publish event`() {
        val user = createDummyUser()
        `when`(userRepository.findActiveUser(defaultEmail)).thenReturn(Optional.of(user))

        authService.initiatePasswordReset(defaultEmail)

        verify(passwordResetTokenRepository).deleteByUser(user)
        verify(passwordResetTokenRepository).save(any<PasswordResetToken>())
        verify(eventPublisher).publishEvent(any<ForgotPasswordEvent>())
    }

    // ==========================================
    // 5. COMPLETE PASSWORD RESET TESTS
    // ==========================================

    @Test
    fun `completePasswordReset should update password and delete token`() {
        val user = createDummyUser()
        val resetToken = PasswordResetToken(1L, "valid-token", user, Instant.now().plusSeconds(900))

        `when`(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(resetToken))
        `when`(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedHash")

        authService.completePasswordReset("valid-token", "newPassword123")

        verify(userRepository).save(check {
            assertEquals("newEncodedHash", it.passwordHash)
        })
        verify(passwordResetTokenRepository).delete(resetToken)
    }

    @Test
    fun `completePasswordReset should throw TokenExpiredException if token is old`() {
        val user = createDummyUser()
        val expiredToken = PasswordResetToken(1L, "expired-token", user, Instant.now().minusSeconds(100))

        `when`(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken))

        assertThrows<TokenExpiredException> {
            authService.completePasswordReset("expired-token", "newPassword123")
        }

        // It should still delete the expired token from DB to clean up
        verify(passwordResetTokenRepository).delete(expiredToken)
        verify(userRepository, never()).save(any())
    }

    // ==========================================
    // 6. LOGOUT TESTS
    // ==========================================

    @Test
    fun `logout should blacklist token and revoke refresh token`() {
        val bearerToken = "Bearer eyJhbGci..."
        val rawToken = "eyJhbGci..."

        `when`(jwtService.extractId(rawToken)).thenReturn(1L)

        authService.logout(bearerToken)

        // Verify it stripped the Bearer prefix
        verify(tokenBlacklistService).blacklistToken(rawToken)
        verify(refreshTokenService).revokeTokensForUser(1L)
    }
}