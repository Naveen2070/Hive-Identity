package com.thehiveproject.identity_service.user

interface UserRepository: org.springframework.data.jpa.repository.JpaRepository<com.thehiveproject.identity_service.user.User, kotlin.Long> {
}