package com.thehiveproject.identity_service.auth.event

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class ForgotPasswordListener {

    private val logger = LoggerFactory.getLogger(ForgotPasswordListener::class.java)

    @Async
    @EventListener
    fun handleForgotPasswordEvent(event: ForgotPasswordEvent) {
        // SIMULATION: This is where we would call SendGrid / SES
        logger.info("📨 [EMAIL SERVICE] Sending Password Reset Email to: ${event.email}")
        logger.info("🔗 Link: https://thehive.com/reset-password?token=${event.resetToken}")

        // Simulate network delay (verify it doesn't block the API)
        Thread.sleep(2000)

        logger.info("✅ [EMAIL SERVICE] Email sent successfully!")
    }
}