package com.thehiveproject.identity_service.config

import com.thehiveproject.identity_service.auth.security.CustomUserDetails
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.security.core.context.SecurityContextHolder
import java.util.*

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
class AuditConfig {
    private val logger = LoggerFactory.getLogger(AuditConfig::class.java)
    @Bean
    fun auditorProvider(): AuditorAware<Long> {
        return AuditorAware {
            val authentication = SecurityContextHolder.getContext().authentication

            // 1. Check for Unauthenticated / Anonymous (e.g. Self-Registration)
            // Result: NULL in database
            if (authentication == null ||
                !authentication.isAuthenticated ||
                authentication.principal == "anonymousUser"
            ) {
                return@AuditorAware Optional.empty()
            }

            // 2. Check for SYSTEM actions
            if (authentication.name == "SYSTEM") {
                return@AuditorAware Optional.of(0L)
            }

            // 3. Extract Real User ID
            try {
                logger.info("entered")
                val userDetails = authentication.principal as CustomUserDetails
                return@AuditorAware Optional.of(userDetails.id)
            } catch (e: Exception) {
                logger.error(e.message, e)
                return@AuditorAware Optional.empty()
            }
        }
    }
}