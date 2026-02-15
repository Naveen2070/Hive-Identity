package com.thehiveproject.identity_service.notification

import com.thehiveproject.identity_service.common.event.EmailNotificationEvent
import com.thehiveproject.identity_service.config.RabbitMQConfig
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

@Service
class NotificationProducer(
    private val rabbitTemplate: RabbitTemplate,
    private val frontendProperties: FrontendProperties
) {

    private val logger = LoggerFactory.getLogger(NotificationProducer::class.java)

    fun sendForgotPasswordEmail(email: String, token: String) {
        val event = EmailNotificationEvent(
            recipientEmail = email,
            subject = "Reset your Password - The Hive",
            templateCode = "PASSWORD_RESET",
            variables = mapOf(
                "token" to token,
                "resetLink" to "${frontendProperties.url}/reset-password?token=$token"
            )
        )

        logger.info("Publishing Email Event to RabbitMQ: ${RabbitMQConfig.QUEUE_EMAIL}")

        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_EMAIL,
                event
            )
        } catch (e: Exception) {
            logger.error("Failed to send email notification", e)
        }
    }
}