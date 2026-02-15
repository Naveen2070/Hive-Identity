package com.thehiveproject.identity_service.common.utils

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

object SystemContext {
    fun runAsSystem(action: () -> Unit) {
        // 1. Create a fake "System" user
        val systemAuth = UsernamePasswordAuthenticationToken("SYSTEM", null, emptyList())

        // 2. Set context
        val previousAuth = SecurityContextHolder.getContext().authentication
        SecurityContextHolder.getContext().authentication = systemAuth

        try {
            // 3. Run the code (Auditor will see "SYSTEM" and return 0)
            action()
        } finally {
            // 4. Cleanup
            SecurityContextHolder.getContext().authentication = previousAuth
        }
    }
}