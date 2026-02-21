package com.thehiveproject.identity_service.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.thehiveproject.identity_service.admin.controller.AdminController
import com.thehiveproject.identity_service.auth.dto.CreateUserRequest
import com.thehiveproject.identity_service.auth.service.JwtService
import com.thehiveproject.identity_service.auth.service.TokenBlacklistService
import com.thehiveproject.identity_service.internal.controller.InternalProperties
import com.thehiveproject.identity_service.user.dto.UserDto
import com.thehiveproject.identity_service.user.exception.UserNotFoundException
import com.thehiveproject.identity_service.user.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(AdminController::class)
@EnableMethodSecurity
class AdminControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var userService: UserService

    @MockitoBean
    private lateinit var jwtService: JwtService

    @MockitoBean
    private lateinit var userDetailsService: UserDetailsService

    @MockitoBean
    private lateinit var tokenBlacklistService: TokenBlacklistService

    @MockitoBean
    private lateinit var internalProperties: InternalProperties

    // --- Helpers ---
    private val adminUri = "/api/admin/users"
    private val dummyUserDto = UserDto(
        id = 1L.toString(),
        email = "admin@test.com",
        fullName = "Admin User",
        active = true,
        roles = setOf(
            UserDto.UserRoleDto(
                active = true,
                id = null,
                roleId = null,
                roleName = null
            )
        )
    )

    // ==========================================
    // 1. SECURITY (RBAC) EDGE CASES
    // ==========================================

    @Test
    fun `should return 401 UNAUTHORIZED if no token is provided`() {
        mockMvc.perform(get(adminUri))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(roles = ["USER", "ORGANIZER"]) // Testing standard roles
    fun `should return 403 FORBIDDEN if user does not have SUPER_ADMIN role`() {
        mockMvc.perform(get(adminUri).with(csrf()))
            .andExpect(status().isForbidden)
    }

    // ==========================================
    // 2. GET ALL USERS (PAGINATED)
    // ==========================================

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `getAllUsers should return paginated 200 OK for SUPER_ADMIN`() {
        val page = PageImpl(listOf(dummyUserDto), PageRequest.of(0, 20), 1)

        `when`(userService.getAllUsers(any<Pageable>(), any())).thenReturn(page)

        mockMvc.perform(
            get(adminUri)
                .param("page", "0")
                .param("size", "20")
                .param("search", "Admin")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content[0].email").value("admin@test.com"))
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    // ==========================================
    // 3. GET USER BY ID
    // ==========================================

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `getUserById should return 200 OK when user exists`() {
        `when`(userService.getUserById(1L)).thenReturn(dummyUserDto)

        mockMvc.perform(get("$adminUri/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("admin@test.com"))
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `getUserById should return 404 NOT FOUND when user does not exist`() {
        `when`(userService.getUserById(99L)).thenThrow(UserNotFoundException("User not found"))

        mockMvc.perform(get("$adminUri/99"))
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `getUserById should return 400 BAD REQUEST if ID is not a number`() {
        // Since the controller does id.toLong(), passing letters will throw a NumberFormatException
        // Spring's global handler should catch this, resulting in a 400.
        mockMvc.perform(get("$adminUri/abc").with(csrf()))
            .andExpect(status().isBadRequest)
    }

    // ==========================================
    // 4. CREATE INTERNAL USER
    // ==========================================

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `createInternalUser should return 201 CREATED with valid payload`() {
        val request = CreateUserRequest(
            email = "newadmin@test.com",
            password = "SecurePassword123!",
            fullName = "New Admin",
            domainAccess = mutableSetOf("ALL"),
            role = "ADMIN"
        )

        `when`(userService.createInternalUser(any())).thenReturn(dummyUserDto.copy(email = "newadmin@test.com"))

        mockMvc.perform(
            post(adminUri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf())
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.email").value("newadmin@test.com"))
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `createInternalUser should return 400 BAD REQUEST if payload fails validation`() {
        // Simulating a bad payload (e.g., missing email, blank fields) based on @Valid rules
        val badRequest = CreateUserRequest(
            email = "not-an-email",
            password = "123",
            fullName = "",
            domainAccess = mutableSetOf("ALL"),
            role = "ADMIN"
        )

        mockMvc.perform(
            post(adminUri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badRequest))
                .with(csrf())
        )
            .andExpect(status().isBadRequest)
    }

    // ==========================================
    // 5. CHANGE USER STATUS (PATCH)
    // ==========================================

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `changeUserStatus should return 200 OK when successfully toggled`() {
        `when`(userService.changeUserStatus(1L, false)).thenReturn(dummyUserDto.copy(active = false))

        mockMvc.perform(
            patch("$adminUri/1/status")
                .param("active", "false")
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.active").value(false))
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `changeUserStatus should return 400 BAD REQUEST if 'active' parameter is missing`() {
        mockMvc.perform(patch("$adminUri/1/status").with(csrf()))

            .andExpect(status().isBadRequest)
    }

    // ==========================================
    // 6. HARD DELETE USER
    // ==========================================

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `hardDeleteUser should return 204 NO CONTENT on success`() {
        mockMvc.perform(delete("$adminUri/1/hard").with(csrf()))
            .andExpect(status().isNoContent)
    }
}