package com.thehiveproject.identity_service.auth

import com.thehiveproject.identity_service.auth.dto.AuthResponse
import com.thehiveproject.identity_service.auth.dto.LoginRequest
import com.thehiveproject.identity_service.auth.dto.RegisterRequest
import com.thehiveproject.identity_service.auth.exception.InvalidCredentialsException
import com.thehiveproject.identity_service.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val authenticationManager: AuthenticationManager,
    private val userDetailsService: UserDetailsService,
    private val jwtService: JwtService
):AuthService {

    private val logger = LoggerFactory.getLogger(AuthServiceImpl::class.java)

    override fun registerUser(user: RegisterRequest): AuthResponse {
        TODO("Not yet implemented")
    }

    override fun login(loginRequest: LoginRequest): AuthResponse {
        try {
            // 1. Authenticate user credentials
            val authenticationToken = UsernamePasswordAuthenticationToken(
                loginRequest.email,
                loginRequest.password
            )

            authenticationManager.authenticate(authenticationToken)

            // 2. Load user details
            val userDetails = userDetailsService.loadUserByUsername(loginRequest.email)
            val user = userRepository.findByEmail(loginRequest.email).get()

            // 3. (Optional) Add custom JWT claims here
            val customClaims = mapOf<String, Any>(
                "id" to user.id!!,
                "email" to user.email,
            )

            // 4. Generate JWT
            val token = jwtService.generateToken(customClaims, userDetails)

            // 5. Return response
            return AuthResponse(
                token = token,
                email = loginRequest.email
            )
        } catch (ex: AuthenticationException) {
            logger.error("AuthenticationException: {}", ex.message, ex)
            throw InvalidCredentialsException("Invalid email or password")
        }
    }
}