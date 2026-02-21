package com.thehiveproject.identity_service.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.thehiveproject.identity_service.auth.controller.AuthController
import com.thehiveproject.identity_service.auth.dto.*
import com.thehiveproject.identity_service.auth.exception.InvalidCredentialsException
import com.thehiveproject.identity_service.auth.exception.InvalidRefreshTokenException
import com.thehiveproject.identity_service.auth.service.AuthService
import com.thehiveproject.identity_service.auth.service.JwtService
import com.thehiveproject.identity_service.auth.service.TokenBlacklistService
import com.thehiveproject.identity_service.internal.controller.InternalProperties
import com.thehiveproject.identity_service.user.exception.UserAlreadyExistsException
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AuthController::class)
@EnableMethodSecurity
class AuthControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var authService: AuthService

    // --- Security Context Mocks (To satisfy the WebMvcTest Slice) ---
    @MockitoBean
    private lateinit var jwtService: JwtService

    @MockitoBean
    private lateinit var userDetailsService: UserDetailsService

    @MockitoBean
    private lateinit var tokenBlacklistService: TokenBlacklistService

    @MockitoBean
    private lateinit var internalProperties: InternalProperties

    // --- Helpers ---
    private val authUri = "/api/auth"
    private val dummyAuthResponse = AuthResponse(
        token = "mock.access.token",
        refreshToken = "mock.refresh.token",
        email = "user.email"
    )

    // ==========================================
    // 1. REGISTER TESTS
    // ==========================================

    @Test
    @WithMockUser
    fun `register should return 200 OK with tokens on success`() {
        val request = RegisterRequest(
            email = "new@test.com",
            password = "SecurePassword123!",
            fullName = "New User"
        )

        `when`(authService.registerUser(any())).thenReturn(dummyAuthResponse)

        mockMvc.perform(
            post("$authUri/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").value("mock.access.token"))
    }

    @Test
    @WithMockUser
    fun `register should return 409 CONFLICT if user already exists`() {
        // Use named arguments to map the fields correctly!
        val request = RegisterRequest(
            email = "exists@test.com",
            password = "SecurePassword123!",
            fullName = "Exists User"
        )

        `when`(authService.registerUser(any())).thenThrow(UserAlreadyExistsException("User already exists"))

        mockMvc.perform(
            post("$authUri/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("User already exists"))
    }

    // ==========================================
    // 2. LOGIN TESTS
    // ==========================================

    @Test
    @WithMockUser
    fun `login should return 200 OK with tokens on valid credentials`() {
        val request = LoginRequest("user@test.com", "correctPassword")

        `when`(authService.login(any())).thenReturn(dummyAuthResponse)

        mockMvc.perform(
            post("$authUri/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").exists())
    }

    @Test
    @WithMockUser
    fun `login should return 401 UNAUTHORIZED on bad credentials`() {
        val request = LoginRequest("user@test.com", "wrongPassword")

        `when`(authService.login(any())).thenThrow(InvalidCredentialsException("Invalid email or password"))

        mockMvc.perform(
            post("$authUri/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    // ==========================================
    // 3. REFRESH TOKEN TESTS
    // ==========================================

    @Test
    @WithMockUser
    fun `refreshAccessToken should return 200 OK with new tokens`() {
        val request = TokenRefreshRequest("valid.refresh.token")

        `when`(authService.refreshToken(any())).thenReturn(dummyAuthResponse)

        mockMvc.perform(
            post("$authUri/refresh")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").exists())
    }

    @Test
    @WithMockUser
    fun `refreshAccessToken should return 401 UNAUTHORIZED for invalid token`() {
        val request = TokenRefreshRequest("invalid.token")

        `when`(authService.refreshToken(any())).thenThrow(InvalidRefreshTokenException("Token is invalid or expired"))

        mockMvc.perform(
            post("$authUri/refresh")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    // ==========================================
    // 4. LOGOUT TESTS
    // ==========================================

    @Test
    @WithMockUser
    fun `logout should return 200 OK and blacklist token`() {
        val token = "Bearer some.jwt.token"

        // doNothing() because logout() returns Unit (void)
        doNothing().`when`(authService).logout(token)

        mockMvc.perform(
            post("$authUri/logout")
                .with(csrf())
                .header("Authorization", token)
        )
            .andExpect(status().isOk)
    }

    @Test
    @WithMockUser
    fun `logout should return 400 BAD REQUEST if Authorization header is missing`() {
        mockMvc.perform(
            post("$authUri/logout")
                .with(csrf())
        ) // Intentionally omitting the header
            .andExpect(status().isBadRequest)
    }

    // ==========================================
    // 5. PASSWORD MANAGEMENT TESTS
    // ==========================================

    @Test
    @WithMockUser
    fun `forgotPassword should always return 200 OK for valid email payload`() {
        val request = ForgotPasswordRequest("user@test.com")

        doNothing().`when`(authService).initiatePasswordReset(any())

        mockMvc.perform(
            post("$authUri/forgot-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
    }

    @Test
    @WithMockUser
    fun `resetPassword should return 200 OK on success`() {
        val request = ResetPasswordRequest("reset-token-123", "NewStrongPass1!")

        doNothing().`when`(authService).completePasswordReset(any(), any())

        mockMvc.perform(
            post("$authUri/reset-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
    }
}