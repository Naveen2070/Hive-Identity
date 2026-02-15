package com.thehiveproject.identity_service.auth.event

import com.thehiveproject.identity_service.notification.NotificationProducer
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class ForgotPasswordListener(
    private val notificationProducer: NotificationProducer
) {

    private val logger = LoggerFactory.getLogger(ForgotPasswordListener::class.java)

    @Async
    @EventListener
    fun handleForgotPasswordEvent(event: ForgotPasswordEvent) {
        logger.info("Received ForgotPasswordEvent: $event")
        notificationProducer.sendForgotPasswordEmail(event.email, event.resetToken)
        logger.info("ForgotPasswordEvent processed successfully")
    }
}