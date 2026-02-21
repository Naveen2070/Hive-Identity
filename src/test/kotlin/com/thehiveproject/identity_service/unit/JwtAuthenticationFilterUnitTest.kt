package com.thehiveproject.identity_service.unit

import com.thehiveproject.identity_service.auth.security.JwtAuthenticationFilter
import com.thehiveproject.identity_service.auth.service.JwtService
import com.thehiveproject.identity_service.auth.service.TokenBlacklistService
import io.jsonwebtoken.ExpiredJwtException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.web.servlet.HandlerExceptionResolver

@ExtendWith(MockitoExtension::class)
class JwtAuthenticationFilterUnitTest {

    @Mock lateinit var jwtService: JwtService
    @Mock lateinit var userDetailsService: UserDetailsService
    @Mock lateinit var resolver: HandlerExceptionResolver
    @Mock lateinit var tokenBlacklistService: TokenBlacklistService

    @InjectMocks
    lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    private lateinit var request: MockHttpServletRequest
    private lateinit var response: MockHttpServletResponse
    private lateinit var filterChain: MockFilterChain

    @BeforeEach
    fun setup() {
        request = MockHttpServletRequest()
        response = MockHttpServletResponse()
        filterChain = MockFilterChain()

        // Ensure a clean slate for the security context before every test
        SecurityContextHolder.clearContext()
    }

    @AfterEach
    fun teardown() {
        // Prevent SecurityContext bleed-over into other tests
        SecurityContextHolder.clearContext()
    }

    // ==========================================
    // 1. MISSING OR INVALID HEADER TESTS
    // ==========================================

    @Test
    fun `should continue filter chain and ignore if Authorization header is missing`() {
        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
        assertNotNull(filterChain.request, "Filter chain should have been called")
    }

    @Test
    fun `should continue filter chain and ignore if Authorization header does not start with Bearer`() {
        request.addHeader("Authorization", "Basic some-base64-string")

        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
        assertNotNull(filterChain.request, "Filter chain should have been called")
    }

    // ==========================================
    // 2. BLACKLISTED TOKEN TEST
    // ==========================================

    @Test
    fun `should return 401 UNAUTHORIZED immediately if token is blacklisted`() {
        val token = "blacklisted-token"
        request.addHeader("Authorization", "Bearer $token")

        `when`(tokenBlacklistService.isBlacklisted(token)).thenReturn(true)

        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        // Verify it stopped the request
        assertNull(filterChain.request, "Filter chain should NOT be called")
        assertNull(SecurityContextHolder.getContext().authentication)

        // Verify the response payload
        assertEquals(401, response.status)
        assertEquals("application/json", response.contentType)
        assertTrue(response.contentAsString.contains("You have logged out"))
    }

    // ==========================================
    // 3. SUCCESSFUL AUTHENTICATION TEST
    // ==========================================

    @Test
    fun `should authenticate user and set SecurityContext if token is valid`() {
        val token = "valid-token"
        val email = "user@test.com"
        request.addHeader("Authorization", "Bearer $token")

        val userDetails = mock(UserDetails::class.java)
        `when`(userDetails.authorities).thenReturn(listOf(SimpleGrantedAuthority("ROLE_USER")))

        `when`(tokenBlacklistService.isBlacklisted(token)).thenReturn(false)
        `when`(jwtService.extractUsername(token)).thenReturn(email)
        `when`(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails)
        `when`(jwtService.isTokenValid(token, userDetails)).thenReturn(true)

        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        // Assert the user was injected into the global SecurityContext
        val auth = SecurityContextHolder.getContext().authentication
        assertNotNull(auth)
        assertTrue(auth is UsernamePasswordAuthenticationToken)
        assertEquals(userDetails, auth.principal)

        // Assert the request was allowed to continue to the controller
        assertNotNull(filterChain.request, "Filter chain SHOULD be called")
    }

    // ==========================================
    // 4. EXPIRED TOKEN TEST
    // ==========================================

    @Test
    fun `should delegate to HandlerExceptionResolver if token is expired`() {
        val token = "expired-token"
        request.addHeader("Authorization", "Bearer $token")

        val expiredException = mock(ExpiredJwtException::class.java)

        `when`(tokenBlacklistService.isBlacklisted(token)).thenReturn(false)
        `when`(jwtService.extractUsername(token)).thenThrow(expiredException)

        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        // Verify the resolver was called to handle the exception gracefully
        verify(resolver).resolveException(eq(request), eq(response), isNull(), eq(expiredException))

        // Assert the request was halted
        assertNull(filterChain.request, "Filter chain should NOT be called")
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    // ==========================================
    // 5. EXISTING AUTHENTICATION TEST
    // ==========================================

    @Test
    fun `should bypass token validation if SecurityContext already has authentication`() {
        val token = "valid-token"
        val email = "user@test.com"
        request.addHeader("Authorization", "Bearer $token")

        // Pre-fill the context (simulating another filter having already authenticated the user)
        val existingAuth = UsernamePasswordAuthenticationToken("existingUser", null, emptyList())
        SecurityContextHolder.getContext().authentication = existingAuth

        `when`(tokenBlacklistService.isBlacklisted(token)).thenReturn(false)
        `when`(jwtService.extractUsername(token)).thenReturn(email)

        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        // Verify it didn't try to load the user from the DB again
        verify(userDetailsService, never()).loadUserByUsername(anyString())

        // Context should remain exactly as it was
        assertEquals(existingAuth, SecurityContextHolder.getContext().authentication)

        // Chain continues
        assertNotNull(filterChain.request, "Filter chain SHOULD be called")
    }
}