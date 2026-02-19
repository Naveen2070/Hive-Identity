package com.thehiveproject.identity_service.notification

import org.jetbrains.annotations.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "frontend")
data class FrontendProperties(
    @field:NotNull
    val url: String
)
