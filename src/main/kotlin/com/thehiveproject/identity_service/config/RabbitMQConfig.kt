package com.thehiveproject.identity_service.config

import org.slf4j.LoggerFactory
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {
    private val logger = LoggerFactory.getLogger(RabbitMQConfig::class.java)

    companion object {
        const val EXCHANGE_NAME = "x.notification"
        const val QUEUE_EMAIL = "q.notification.email"
        const val ROUTING_KEY_EMAIL = "k.notification.email"
    }

    // 1. This bean forces Spring to create the Queue/Exchange in RabbitMQ
    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    // 2. Queue
    @Bean
    fun emailQueue(): Queue {
        return Queue(QUEUE_EMAIL, true)
    }

    // 3. Exchange
    @Bean
    fun exchange(): DirectExchange {
        return DirectExchange(EXCHANGE_NAME)
    }

    // 4. Binding
    @Bean
    fun binding(emailQueue: Queue, exchange: DirectExchange): Binding {
        return BindingBuilder.bind(emailQueue)
            .to(exchange)
            .with(ROUTING_KEY_EMAIL)
    }

    @Bean
    fun messageConverter(): MessageConverter {
        return Jackson2JsonMessageConverter()
    }

    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory, messageConverter: MessageConverter): RabbitTemplate {
        val template = RabbitTemplate(connectionFactory)
        template.messageConverter = messageConverter
        return template
    }

    @Bean
    fun rabbitMqInitializer(admin: RabbitAdmin, emailQueue: Queue, exchange: DirectExchange, binding: Binding): ApplicationRunner {
        return ApplicationRunner {
            logger.info("INIT: Attempting to declare RabbitMQ infrastructure...")
            try {
                admin.declareQueue(emailQueue)
                admin.declareExchange(exchange)
                admin.declareBinding(binding)
                logger.info("INIT: RabbitMQ Queue/Exchange/Binding declared successfully!")
            } catch (e: Exception) {
                logger.error("INIT FAILED: Could not create RabbitMQ infrastructure.", e)
            }
        }
    }
}