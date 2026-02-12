package com.thehiveproject.identity_service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class IdentityServiceApplication

fun main(args: Array<String>) {
	runApplication<IdentityServiceApplication>(*args)
}
