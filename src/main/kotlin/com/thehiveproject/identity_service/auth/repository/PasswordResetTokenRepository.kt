package com.thehiveproject.identity_service.auth.repository

import com.thehiveproject.identity_service.auth.entity.PasswordResetToken
import com.thehiveproject.identity_service.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, Long> {
    fun findByToken(token: String): Optional<PasswordResetToken>
    fun deleteByUser(user: User)
}