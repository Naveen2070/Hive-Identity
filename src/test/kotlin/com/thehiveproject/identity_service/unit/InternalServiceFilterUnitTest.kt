package com.thehiveproject.identity_service.unit

import com.thehiveproject.identity_service.auth.security.InternalServiceFilter
import com.thehiveproject.identity_service.common.utils.S2SAuthUtil
import com.thehiveproject.identity_service.internal.controller.InternalProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

@ExtendWith(MockitoExtension::class)
class InternalServiceFilterUnitTest {

    private lateinit var filter: InternalServiceFilter
    private lateinit var request: MockHttpServletRequest
    private lateinit var response: MockHttpServletResponse
    private lateinit var filterChain: MockFilterChain

    private val testSharedSecret = "super-secret-s2s-key"

    @BeforeEach
    fun setup() {
        // Mock the properties to provide our test secret
        val properties = mock(InternalProperties::class.java)
        `when`(properties.sharedSecret).thenReturn(testSharedSecret)

        filter = InternalServiceFilter(properties)

        // Initialize Spring's Mock Servlet objects
        request = MockHttpServletRequest()
        response = MockHttpServletResponse()
        filterChain = MockFilterChain()
    }

    // ==========================================
    // 1. NON-INTERNAL PATH TEST
    // ==========================================

    @Test
    fun `should bypass filter completely if path is not internal`() {
        request.requestURI = "/api/auth/login" // Public path

        filter.doFilter(request, response, filterChain)

        // Verify that it passed the request down the chain without touching response status
        assertNotNull(filterChain.request, "Filter chain should have been called")
        assertEquals(200, response.status)
    }

    // ==========================================
    // 2. MISSING HEADER TESTS
    // ==========================================

    @Test
    fun `should return 403 FORBIDDEN if required S2S headers are missing`() {
        request.requestURI = "/api/internal/users/batch"
        // Intentionally NOT adding the headers

        filter.doFilter(request, response, filterChain)

        assertEquals(403, response.status)
        assertEquals("Missing Internal Headers", response.errorMessage)
        assertNull(filterChain.request, "Filter chain should NOT be called")
    }

    @Test
    fun `should return 400 BAD REQUEST if timestamp is not a valid number`() {
        request.requestURI = "/api/internal/users/batch"
        request.addHeader("X-Internal-Service-ID", "core-api")
        request.addHeader("X-Service-Signature", "some-signature")
        request.addHeader("X-Service-Timestamp", "not-a-number") // Malformed

        filter.doFilter(request, response, filterChain)

        assertEquals(400, response.status)
        assertEquals("Malformed Timestamp", response.errorMessage)
        assertNull(filterChain.request, "Filter chain should NOT be called")
    }

    // ==========================================
    // 3. HMAC SIGNATURE VALIDATION TESTS
    // ==========================================

    @Test
    fun `should return 403 FORBIDDEN if signature is invalid or expired`() {
        val reqServiceId = "core-api"
        val reqSignature = "this-is-a-fake-bad-signature"
        // Use the current time so it passes the timestamp check, but fails the signature check
        val currentTimestamp = java.time.Instant.now().epochSecond.toString()

        request.requestURI = "/api/internal/users/batch"
        request.addHeader("X-Internal-Service-ID", reqServiceId)
        request.addHeader("X-Service-Signature", reqSignature)
        request.addHeader("X-Service-Timestamp", currentTimestamp)

        // NO MOCKING NEEDED! Let the real S2SAuthUtil reject the fake signature.
        filter.doFilter(request, response, filterChain)

        assertEquals(403, response.status)
        assertEquals("Invalid or Expired Internal Token", response.errorMessage)
        assertNull(filterChain.request, "Filter chain should NOT be called")
    }

    @Test
    fun `should proceed down filter chain if signature is perfectly valid`() {
        val reqServiceId = "core-api"
        val currentTimestamp = java.time.Instant.now().epochSecond

        // Use the REAL utility to generate a mathematically valid signature for the test
        val validSignature = S2SAuthUtil.generateSignature(
            serviceId = reqServiceId,
            timestamp = currentTimestamp,
            sharedSecret = testSharedSecret // Must match the secret in the @BeforeEach setup
        )

        request.requestURI = "/api/internal/users/batch"
        request.addHeader("X-Internal-Service-ID", reqServiceId)
        request.addHeader("X-Service-Signature", validSignature)
        request.addHeader("X-Service-Timestamp", currentTimestamp.toString())

        // The filter will use the real S2SAuthUtil, which will perfectly validate the signature we just made
        filter.doFilter(request, response, filterChain)

        assertEquals(200, response.status)
        assertNotNull(filterChain.request, "Filter chain SHOULD be called")
    }
}