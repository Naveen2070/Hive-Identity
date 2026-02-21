package com.thehiveproject.identity_service.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.thehiveproject.identity_service.auth.exception.InvalidPasswordException
import com.thehiveproject.identity_service.auth.security.CustomUserDetails
import com.thehiveproject.identity_service.auth.service.JwtService
import com.thehiveproject.identity_service.auth.service.TokenBlacklistService
import com.thehiveproject.identity_service.internal.controller.InternalProperties
import com.thehiveproject.identity_service.user.controller.UserController
import com.thehiveproject.identity_service.user.dto.ChangePasswordRequest
import com.thehiveproject.identity_service.user.dto.UpdateProfileRequest
import com.thehiveproject.identity_service.user.dto.UserResponse
import com.thehiveproject.identity_service.user.exception.UserNotFoundException
import com.thehiveproject.identity_service.user.service.UserService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@WebMvcTest(UserController::class)
@EnableMethodSecurity
class UserControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var userService: UserService

    // --- Security Context Mocks ---
    @MockitoBean
    private lateinit var jwtService: JwtService

    @MockitoBean
    private lateinit var userDetailsService: UserDetailsService

    @MockitoBean
    private lateinit var tokenBlacklistService: TokenBlacklistService

    @MockitoBean
    private lateinit var internalProperties: InternalProperties

    private val userUri = "/api/users"
    private val testUsername = "user@test.com"

    private val dummyUserResponse = UserResponse(
        id = 1L.toString(),
        email = testUsername,
        fullName = "Test User",
        roles = listOf("USER"),
        domainAccess = setOf("events"),
        createdAt = Instant.now(),
        isActive = true
    )

    private lateinit var mockCustomUser: CustomUserDetails

    @BeforeEach
    fun setup() {
        // Mock the CustomUserDetails to prevent ClassCastExceptions in DELETE endpoints
        mockCustomUser = mock(CustomUserDetails::class.java)
        `when`(mockCustomUser.username).thenReturn(testUsername)
        `when`(mockCustomUser.password).thenReturn("password")
    }

    // ==========================================
    // 1. GET /me (Current User Profile)
    // ==========================================

    @Test
    @WithMockUser(username = "user@test.com")
    fun `getCurrentUser should return 200 OK with profile data`() {
        `when`(userService.getUserProfile(testUsername)).thenReturn(dummyUserResponse)

        mockMvc.perform(get("$userUri/me"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value(testUsername))
            .andExpect(jsonPath("$.fullName").value("Test User"))
    }

    @Test
    @WithMockUser(username = "ghost@test.com")
    fun `getCurrentUser should return 404 NOT FOUND if user does not exist in DB`() {
        `when`(userService.getUserProfile("ghost@test.com")).thenThrow(UserNotFoundException("User not found"))

        mockMvc.perform(get("$userUri/me"))
            .andExpect(status().isNotFound)
    }

    // ==========================================
    // 2. PATCH /me (Update Profile)
    // ==========================================

    @Test
    @WithMockUser(username = "user@test.com")
    fun `updateProfile should return 200 OK with updated data`() {
        val request = UpdateProfileRequest(fullName = "Updated Name")
        val updatedResponse = dummyUserResponse.copy(fullName = "Updated Name")

        `when`(userService.updateProfile(testUsername, request)).thenReturn(updatedResponse)

        mockMvc.perform(
            patch("$userUri/me")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fullName").value("Updated Name"))
    }

    // ==========================================
    // 3. POST /change-password
    // ==========================================

    @Test
    @WithMockUser(username = "user@test.com")
    fun `changePassword should return 200 OK on success`() {
        val request = ChangePasswordRequest(
            oldPassword = "OldPassword123!",
            newPassword = "NewStrongPassword1!"
        )

        doNothing().`when`(userService).changePassword(any(), any())

        mockMvc.perform(
            post("$userUri/change-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
    }

    @Test
    @WithMockUser(username = "user@test.com")
    fun `changePassword should return 403 FORBIDDEN if old password is incorrect`() {
        val request = ChangePasswordRequest(
            oldPassword = "WrongPassword!",
            newPassword = "NewStrongPassword1!"
        )

        `when`(
            userService.changePassword(
                any(),
                any()
            )
        ).thenThrow(InvalidPasswordException("Old password is incorrect"))

        mockMvc.perform(
            post("$userUri/change-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Old password is incorrect"))
    }

    // ==========================================
    // 4. DELETE /deactivate/me (Deactivate Account)
    // ==========================================

    @Test
    fun `deactivateAccount should return 204 NO CONTENT on success`() {
        doNothing().`when`(userService).deactivateAccount(testUsername)

        mockMvc.perform(
            delete("$userUri/deactivate/me")
                .with(user(mockCustomUser))
                .with(csrf())
        )
            .andExpect(status().isNoContent)
    }

    // ==========================================
    // 5. DELETE /me (Delete Account)
    // ==========================================

    @Test
    fun `deleteAccount should return 204 NO CONTENT on success`() {
        doNothing().`when`(userService).deleteAccount(testUsername)

        mockMvc.perform(
            delete("$userUri/me")
                .with(user(mockCustomUser))
                .with(csrf())
        )
            .andExpect(status().isNoContent)
    }
}