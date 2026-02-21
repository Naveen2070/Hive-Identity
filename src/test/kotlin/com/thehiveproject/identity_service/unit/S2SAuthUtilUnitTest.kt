package com.thehiveproject.identity_service.unit

import com.thehiveproject.identity_service.common.utils.S2SAuthUtil
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class S2SAuthUtilUnitTest {

    private val testSharedSecret = "this-is-a-very-secure-test-secret-key-2026"
    private val testServiceId = "core-events-api"

    // ==========================================
    // 1. HAPPY PATH & BASIC INTEGRITY
    // ==========================================

    @Test
    fun `validateToken should return true for a perfectly valid signature`() {
        val currentTimestamp = Instant.now().epochSecond
        val signature = S2SAuthUtil.generateSignature(testServiceId, currentTimestamp, testSharedSecret)

        assertTrue(S2SAuthUtil.validateToken(signature, testServiceId, currentTimestamp, testSharedSecret))
    }

    // ==========================================
    // 2. EXACT TIME BOUNDARY TESTING
    // ==========================================

    @Test
    fun `validateToken should perfectly respect exact time boundaries`() {
        val now = Instant.now().epochSecond
        val maxAge = 60L // Default clock skew

        // Case A: Exactly on the boundary (Past) -> SHOULD PASS
        val exactPastBoundary = now - maxAge
        val sigPast = S2SAuthUtil.generateSignature(testServiceId, exactPastBoundary, testSharedSecret)
        assertTrue(S2SAuthUtil.validateToken(sigPast, testServiceId, exactPastBoundary, testSharedSecret))

        // Case B: Exactly on the boundary (Future) -> SHOULD PASS
        val exactFutureBoundary = now + maxAge
        val sigFuture = S2SAuthUtil.generateSignature(testServiceId, exactFutureBoundary, testSharedSecret)
        assertTrue(S2SAuthUtil.validateToken(sigFuture, testServiceId, exactFutureBoundary, testSharedSecret))

        // Case C: 1 second out of bounds (Past) -> SHOULD FAIL
        val outOfBoundsPast = now - maxAge - 1L
        val sigOutPast = S2SAuthUtil.generateSignature(testServiceId, outOfBoundsPast, testSharedSecret)
        assertFalse(S2SAuthUtil.validateToken(sigOutPast, testServiceId, outOfBoundsPast, testSharedSecret))

        // Case D: 1 second out of bounds (Future) -> SHOULD FAIL
        val outOfBoundsFuture = now + maxAge + 1L
        val sigOutFuture = S2SAuthUtil.generateSignature(testServiceId, outOfBoundsFuture, testSharedSecret)
        assertFalse(S2SAuthUtil.validateToken(sigOutFuture, testServiceId, outOfBoundsFuture, testSharedSecret))
    }

    // ==========================================
    // 3. EXTREME DATA & UTF-8 ATTACKS
    // ==========================================

    @Test
    fun `generate and validate should handle extreme UTF-8 and special characters`() {
        val currentTimestamp = Instant.now().epochSecond

        // Simulating a service ID or secret containing emojis, cyrillic, and massive whitespace
        val crazyServiceId = "service-\uD83D\uDE80-测试-АБВГД\n\t   !@#$%^&*()"
        val crazySecret = "secret-\uD83D\uDD12-パスワード-\n\r\t"

        val signature = S2SAuthUtil.generateSignature(crazyServiceId, currentTimestamp, crazySecret)

        assertTrue(
            S2SAuthUtil.validateToken(signature, crazyServiceId, currentTimestamp, crazySecret),
            "Failed to validate complex UTF-8 payloads safely."
        )
    }

    @Test
    fun `validateToken should securely handle empty strings by either processing or rejecting them`() {
        val currentTimestamp = Instant.now().epochSecond

        // 1. Empty Service ID - The crypto library allows this (Payload becomes ":timestamp")
        val signatureWithEmptyServiceId = S2SAuthUtil.generateSignature("", currentTimestamp, testSharedSecret)

        // It should validate if everything matches exactly
        assertTrue(S2SAuthUtil.validateToken(signatureWithEmptyServiceId, "", currentTimestamp, testSharedSecret))

        // But it MUST fail if someone tries to validate an empty payload with a real service ID
        assertFalse(S2SAuthUtil.validateToken(signatureWithEmptyServiceId, testServiceId, currentTimestamp, testSharedSecret))

        // 2. Empty Shared Secret - Java's crypto library strictly forbids 0-length keys
        assertThrows<IllegalArgumentException> {
            // Attempting to generate or validate with an empty secret should instantly throw an error
            S2SAuthUtil.generateSignature(testServiceId, currentTimestamp, "")
        }
    }
    // ==========================================
    // 4. AUTOMATED BRUTE-FORCE MUTATION TESTING
    // ==========================================

    @Test
    fun `brute force mutation - changing any single character in the signature must fail`() {
        val currentTimestamp = Instant.now().epochSecond
        val originalSignature = S2SAuthUtil.generateSignature(testServiceId, currentTimestamp, testSharedSecret)

        // Systematically iterate through the Base64 signature
        for (i in originalSignature.indices) {
            val charArray = originalSignature.toCharArray()

            // Mutate the character slightly (shift its ASCII value)
            // If it's 'A', make it 'B'. If it's 'Z', make it 'Y'.
            charArray[i] = if (charArray[i] == 'A') 'B' else 'A'
            val mutatedSignature = String(charArray)

            // Assert that the system catches EVERY SINGLE 1-character discrepancy
            assertFalse(
                S2SAuthUtil.validateToken(mutatedSignature, testServiceId, currentTimestamp, testSharedSecret),
                "VULNERABILITY: Signature accepted after mutating character at index $i"
            )
        }
    }

    @Test
    fun `brute force injection - appending or prepending data must fail`() {
        val currentTimestamp = Instant.now().epochSecond
        val signature = S2SAuthUtil.generateSignature(testServiceId, currentTimestamp, testSharedSecret)

        // Appending
        assertFalse(S2SAuthUtil.validateToken("$signature=", testServiceId, currentTimestamp, testSharedSecret))
        assertFalse(S2SAuthUtil.validateToken(signature + "A", testServiceId, currentTimestamp, testSharedSecret))

        // Prepending
        assertFalse(S2SAuthUtil.validateToken("A$signature", testServiceId, currentTimestamp, testSharedSecret))
        assertFalse(S2SAuthUtil.validateToken("=$signature", testServiceId, currentTimestamp, testSharedSecret))
    }

    @Test
    fun `brute force timing attack check - identical lengths but different secrets must fail`() {
        val currentTimestamp = Instant.now().epochSecond

        // Create two secrets of the exact same length to ensure the HMAC outputs identical lengths
        val secret1 = "1234567890abcdef"
        val secret2 = "0987654321fedcba"

        val signatureWithSecret1 = S2SAuthUtil.generateSignature(testServiceId, currentTimestamp, secret1)

        // Attempt to validate the signature created by secret1 using secret2
        assertFalse(
            S2SAuthUtil.validateToken(signatureWithSecret1, testServiceId, currentTimestamp, secret2),
            "VULNERABILITY: Accepted a signature generated by a different key of the same length."
        )
    }
}