package com.thehiveproject.identity_service.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.thehiveproject.identity_service.auth.service.JwtService
import com.thehiveproject.identity_service.auth.service.TokenBlacklistService
import com.thehiveproject.identity_service.common.utils.S2SAuthUtil
import com.thehiveproject.identity_service.internal.controller.InternalProperties
import com.thehiveproject.identity_service.internal.controller.InternalUserController
import com.thehiveproject.identity_service.user.dto.UserSummary
import com.thehiveproject.identity_service.user.exception.UserNotFoundException
import com.thehiveproject.identity_service.user.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@WebMvcTest(InternalUserController::class)
@EnableMethodSecurity
@Import(InternalUserControllerIntegrationTest.MockPropertiesConfig::class) // 1. Inject our pre-stubbed config!
class InternalUserControllerIntegrationTest {

    // 2. We create the mock and stub it BEFORE Spring creates the InternalServiceFilter
    @TestConfiguration
    class MockPropertiesConfig {
        @Bean
        fun internalProperties(): InternalProperties {
            val mockProps = mock(InternalProperties::class.java)
            `when`(mockProps.sharedSecret).thenReturn("super-secret-test-key-123")
            return mockProps
        }
    }

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

    // NOTE: @MockitoBean internalProperties and @BeforeEach have been completely removed!

    private val internalUri = "/api/internal/users"

    private val dummyUserSummary = UserSummary(
        id = 1L,
        fullName = "Internal User",
        email = "internal@test.com"
    )

    private val testSecret = "super-secret-test-key-123"

    // ==========================================
    // S2S AUTHENTICATION HELPER
    // ==========================================
    private fun getValidS2SHeaders(): HttpHeaders {
        val timestamp = Instant.now().epochSecond
        val serviceId = "test-service"

        // Generate a valid HMAC-SHA256 signature using the same secret we gave the filter
        val signature = S2SAuthUtil.generateSignature(serviceId, timestamp, testSecret)

        // Attach headers
        val headers = HttpHeaders()
        headers.add("X-Internal-Service-ID", serviceId)
        headers.add("X-Service-Timestamp", timestamp.toString())
        headers.add("X-Service-Signature", signature)

        return headers
    }

    // ==========================================
    // 1. GET USER BY ID TESTS
    // ==========================================

    @Test
    @WithMockUser
    fun `getUserById should return 200 OK with user summary`() {
        `when`(userService.getUserSummaryById(1L)).thenReturn(dummyUserSummary)

        mockMvc.perform(
            get("$internalUri/1")
                .headers(getValidS2SHeaders())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("internal@test.com"))
            .andExpect(jsonPath("$.fullName").value("Internal User"))
    }

    @Test
    @WithMockUser
    fun `getUserById should return 404 NOT FOUND if user does not exist`() {
        `when`(userService.getUserSummaryById(99L)).thenThrow(UserNotFoundException("User not found"))

        mockMvc.perform(
            get("$internalUri/99")
                .headers(getValidS2SHeaders())
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser
    fun `getUserById should return 400 BAD REQUEST if ID is not a valid number`() {
        mockMvc.perform(
            get("$internalUri/abc")
                .headers(getValidS2SHeaders())
        )
            .andExpect(status().isBadRequest)
    }

    // ==========================================
    // 2. BATCH RESOLVE USERS TESTS
    // ==========================================

    @Test
    @WithMockUser
    fun `resolveUsers should return 200 OK with list of summaries`() {
        val requestIds = listOf(1L, 2L)
        val summaries = listOf(
            dummyUserSummary,
            UserSummary(2L, "Second User", "second@test.com")
        )

        `when`(userService.findBatchUserSummary(requestIds)).thenReturn(summaries)

        mockMvc.perform(
            post("$internalUri/batch")
                .headers(getValidS2SHeaders())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestIds))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].email").value("internal@test.com"))
            .andExpect(jsonPath("$[1].email").value("second@test.com"))
    }

    @Test
    @WithMockUser
    fun `resolveUsers should return 400 BAD REQUEST on invalid payload`() {
        val invalidPayload = mapOf("id" to 1L)

        mockMvc.perform(
            post("$internalUri/batch")
                .headers(getValidS2SHeaders())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidPayload))
        )
            .andExpect(status().isBadRequest)
    }
}