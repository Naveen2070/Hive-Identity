package com.thehiveproject.identity_service.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
class AuditConfig {

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
            // Result: 0 in database (We use a convention: Name must be "SYSTEM")
            if (authentication.name == "SYSTEM") {
                return@AuditorAware Optional.of(0L)
            }

            // 3. Extract Real User ID
            try {
                // TODO: Once we implement CustomUserDetails, uncomment this:
                // val userDetails = authentication.principal as CustomUserDetails
                // return@AuditorAware Optional.of(userDetails.id)

                return@AuditorAware Optional.empty()
            } catch (e: Exception) {
                return@AuditorAware Optional.empty()
            }
        }
    }
}