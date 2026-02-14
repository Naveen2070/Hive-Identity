package com.thehiveproject.identity_service.auth

import com.thehiveproject.identity_service.user.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, Long> {
    fun findByToken(token: String): Optional<PasswordResetToken>
    fun deleteByUser(user: User)
}