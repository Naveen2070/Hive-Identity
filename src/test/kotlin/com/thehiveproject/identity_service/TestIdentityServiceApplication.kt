package com.thehiveproject.identity_service

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
    fromApplication<IdentityServiceApplication>().with(TestcontainersConfiguration::class).run(*args)
}