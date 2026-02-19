package com.thehiveproject.identity_service.auth.event

import org.springframework.context.ApplicationEvent

class ForgotPasswordEvent(
    source: Any,
    val email: String,
    val resetToken: String
) : ApplicationEvent(source)