package com.thehiveproject.identity_service.auth

import com.thehiveproject.identity_service.auth.exception.InvalidRefreshTokenException
import com.thehiveproject.identity_service.auth.exception.TokenExpiredException
import com.thehiveproject.identity_service.config.JwtProperties
import com.thehiveproject.identity_service.user.User
import com.thehiveproject.identity_service.user.UserRepository
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.*

@Service
class RefreshTokenServiceImpl(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository,
    private val jwtProperties: JwtProperties
) : RefreshTokenService {
    override fun createRefreshToken(userId: Long): String {
        revokeTokensForUser(userId)

        val refreshToken = RefreshToken(
            user = userRepository.getReferenceById(userId),
            token = UUID.randomUUID().toString(),
            expiryDate = Instant.now().plusMillis(getRefreshTokenDurationMs())
        )

        val tokenEntity = refreshTokenRepository.save(refreshToken)

        return tokenEntity.token
    }

    override fun verifyAndGetUserId(token: String): User {
        val tokenEntity = refreshTokenRepository.findByToken(token)
        if (tokenEntity.isEmpty) {
            throw InvalidRefreshTokenException("Invalid refresh token")
        }
        if (tokenEntity.get().expiryDate.isBefore(Instant.now())) {
            refreshTokenRepository.delete(tokenEntity.get())
            throw TokenExpiredException("Refresh token was expired. Please make a new signin request")
        }
        return tokenEntity.get().user
    }

    override fun revokeTokensForUser(userId: Long) {
        refreshTokenRepository.deleteByUserId(userId)
    }

    private fun getRefreshTokenDurationMs(): Long {
        return jwtProperties.expirationMs + Duration.ofDays(10).toMillis()
    }

}

