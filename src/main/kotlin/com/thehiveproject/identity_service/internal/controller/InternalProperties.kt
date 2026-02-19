package com.thehiveproject.identity_service.internal.controller

import org.jetbrains.annotations.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "internal")
data class InternalProperties(
    @field:NotNull
    val sharedSecret: String
)
