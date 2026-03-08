package com.thehiveproject.identity_service.auth.service

import com.thehiveproject.identity_service.auth.dto.AuthResponse
import com.thehiveproject.identity_service.auth.dto.LoginRequest
import com.thehiveproject.identity_service.auth.dto.RegisterRequest
import com.thehiveproject.identity_service.auth.dto.TokenRefreshRequest
import com.thehiveproject.identity_service.auth.entity.PasswordResetToken
import com.thehiveproject.identity_service.auth.event.ForgotPasswordEvent
import com.thehiveproject.identity_service.auth.exception.InvalidCredentialsException
import com.thehiveproject.identity_service.auth.exception.TokenExpiredException
import com.thehiveproject.identity_service.auth.repository.PasswordResetTokenRepository
import com.thehiveproject.identity_service.auth.security.CustomUserDetails
import com.thehiveproject.identity_service.user.entity.User
import com.thehiveproject.identity_service.user.exception.UserAlreadyExistsException
import com.thehiveproject.identity_service.user.repository.UserRepository
import com.thehiveproject.identity_service.user.service.UserFactory
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

private const val ALLOWED_SIGNUP_ROLES = "USER,ORGANIZER"

@Service
class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val authenticationManager: AuthenticationManager,
    private val jwtService: JwtService,
    private val refreshTokenService: RefreshTokenService,
    private val passwordEncoder: PasswordEncoder,
    private val tokenBlacklistService: TokenBlacklistService,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val userFactory: UserFactory
) : AuthService {

    private val logger = LoggerFactory.getLogger(AuthServiceImpl::class.java)


    @Transactional
    override fun registerUser(user: RegisterRequest): AuthResponse {
        if (userRepository.findByEmail(user.email).isPresent) {
            throw UserAlreadyExistsException("User already exists")
        }

        // 1. Validate all requested roles
        val allowedRoles = ALLOWED_SIGNUP_ROLES.split(",").toSet()
        user.domainRoles.values.forEach { requestedRole ->
            if (!allowedRoles.contains(requestedRole)) {
                throw IllegalArgumentException("Invalid role requested: $requestedRole")
            }
        }

        // 2. Create User Object with Roles
        val newUser = userFactory.createUser(user.email, user.password, user.fullName, user.domainRoles)

        try {
            val savedUser = userRepository.save(newUser)

            // Spring Security Authorities (Flattened for internal context)
            val authorities = savedUser.roles.map {
                SimpleGrantedAuthority("ROLE_${it.role.name}")
            }

            val userDetails = CustomUserDetails(
                savedUser.id!!, savedUser.email, savedUser.passwordHash,
                savedUser.isEnabled(),
                accountNonExpired = true,
                credentialsNonExpired = true,
                accountNonLocked = true,
                authorities = authorities
            )

            return createAuthResponse(savedUser, userDetails)

        } catch (e: Exception) {
            logger.error(e.message)
            throw e
        }
    }

    @Transactional
    override fun login(loginRequest: LoginRequest): AuthResponse {
        try {
            val userEntity = userRepository.findByEmail(loginRequest.email)
                .orElseThrow { UsernameNotFoundException("User not found with email: ${loginRequest.email}") }

            val authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(
                    loginRequest.email,
                    loginRequest.password
                )
            )

            val userDetails = authentication.principal as CustomUserDetails

            return createAuthResponse(userEntity, userDetails)

        } catch (ex: AuthenticationException) {
            logger.error("Authentication failed for ${loginRequest.email}", ex)
            throw ex
        } catch (ex: Exception) {
            logger.error("Authentication failed for ${loginRequest.email}", ex)
            throw InvalidCredentialsException("Invalid email or password")
        }
    }

    @Transactional
    override fun refreshToken(request: TokenRefreshRequest): AuthResponse {
        val user = refreshTokenService.verifyAndGetUserId(request.refreshToken)

        val authorities = user.roles.map {
            SimpleGrantedAuthority("ROLE_${it.role.name}")
        }

        val userDetails = CustomUserDetails(
            user.id!!,
            user.email,
            user.passwordHash,
            user.isEnabled(),
            accountNonExpired = true,
            credentialsNonExpired = true,
            accountNonLocked = true,
            authorities = authorities
        )

        return createAuthResponse(user, userDetails, request.refreshToken)
    }

    private fun createAuthResponse(
        user: User,
        userDetails: CustomUserDetails,
        providedRefreshToken: String? = null
    ): AuthResponse {
        // Build the Multi-Tenant JWT Claim Payload
        val permissionsMap = user.roles.groupBy({ it.domain }, { "ROLE_${it.role.name}" })

        val customClaims = mapOf(
            "id" to user.id!!,
            "email" to user.email,
            "domains" to permissionsMap.keys.toList(),
            "permissions" to permissionsMap
        )

        val token = jwtService.generateToken(customClaims, userDetails)
        val refreshToken = providedRefreshToken ?: refreshTokenService.createRefreshToken(user.id!!)

        return AuthResponse(token, refreshToken, user.email)
    }

    @Transactional
    override fun initiatePasswordReset(email: String) {
        val user = userRepository.findActiveUser(email)
        if (user.isEmpty) {
            logger.info("Password reset requested for {}", email)
            return
        }


        // 1. Delete any old reset tokens for this user
        passwordResetTokenRepository.deleteByUser(user.get())

        // 2. Create new secure token (valid for 15 mins)
        val resetToken = PasswordResetToken(
            token = UUID.randomUUID().toString(),
            user = user.get(),
            expiryDate = Instant.now().plusSeconds(900)
        )
        passwordResetTokenRepository.save(resetToken)

        // 3. Publish Event to RabbitMQ
        eventPublisher.publishEvent(ForgotPasswordEvent(user.get(), user.get().email, resetToken.token))
        logger.info("Password reset token generated for ${email}: ${resetToken.token}")
    }

    @Transactional
    override fun completePasswordReset(tokenString: String, newPassword: String) {
        val resetToken = passwordResetTokenRepository.findByToken(tokenString)
            .orElseThrow { InvalidCredentialsException("Invalid password reset token") }

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken)
            throw TokenExpiredException("Password reset token has expired")
        }

        val user = resetToken.user
        user.passwordHash = passwordEncoder.encode(newPassword)
        userRepository.save(user)

        // Consume the token so it can't be used again
        passwordResetTokenRepository.delete(resetToken)
    }

    @Transactional
    override fun logout(token: String) {
        val jwt = if (token.startsWith("Bearer ")) token.substring(7) else token
        tokenBlacklistService.blacklistToken(jwt)

        val userId = jwtService.extractId(jwt)
        refreshTokenService.revokeTokensForUser(userId)
    }
}