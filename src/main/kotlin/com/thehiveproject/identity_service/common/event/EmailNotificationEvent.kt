package com.thehiveproject.identity_service.common.event

import java.io.Serializable

data class EmailNotificationEvent(
    val recipientEmail: String,
    val subject: String,
    val templateCode: String,
    val variables: Map<String, String>
) : Serializable