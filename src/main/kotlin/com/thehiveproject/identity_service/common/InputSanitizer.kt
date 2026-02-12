package com.thehiveproject.identity_service.common

import org.owasp.encoder.Encode

/**
 * Sanitizes a string for safe HTML output to prevent XSS.
 */
fun sanitizeForHtml(input: String?): String = input?.let { Encode.forHtml(it) } ?: ""