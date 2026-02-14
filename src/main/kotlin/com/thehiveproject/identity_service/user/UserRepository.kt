package com.thehiveproject.identity_service.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): Optional<User>

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.active = true AND u.deleted = false")
    fun findActiveUser(email: String): Optional<User>

    fun existsByEmail(email: String): Boolean
}