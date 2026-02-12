package com.thehiveproject.identity_service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
class IdentityServiceApplication

fun main(args: Array<String>) {
	runApplication<IdentityServiceApplication>(*args)
}
