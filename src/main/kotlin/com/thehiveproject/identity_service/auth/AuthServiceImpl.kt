package com.thehiveproject.identity_service.auth

import com.thehiveproject.identity_service.auth.dto.AuthResponse
import com.thehiveproject.identity_service.auth.dto.LoginRequest
import com.thehiveproject.identity_service.auth.dto.RegisterRequest
import com.thehiveproject.identity_service.auth.exception.InvalidCredentialsException
import com.thehiveproject.identity_service.user.RoleRepository
import com.thehiveproject.identity_service.user.User
import com.thehiveproject.identity_service.user.UserRepository
import com.thehiveproject.identity_service.user.exception.RoleNotFoundException
import com.thehiveproject.identity_service.user.exception.UserAlreadyExistsException
import com.thehiveproject.identity_service.user.exception.UserNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

private const val ALLOWED_SIGNUP_ROLES = "USER,ORGANIZER"

@Service
class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val authenticationManager: AuthenticationManager,
    private val userDetailsService: UserDetailsService,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder
) : AuthService {

    private val logger = LoggerFactory.getLogger(AuthServiceImpl::class.java)


    override fun registerUser(user: RegisterRequest): AuthResponse {
        val userEntity = userRepository.findByEmail(user.email)
        if (userEntity.isPresent) {
            throw UserAlreadyExistsException("User already exists")
        }

        val newUser = User(
            email = user.email,
            passwordHash = passwordEncoder.encode(user.password),
            fullName = user.fullName,
            domainAccess = user.domainAccess
        )

        val allowedRoles = ALLOWED_SIGNUP_ROLES.split(",").toSet()
        if (!allowedRoles.contains(user.role)) {
            throw IllegalArgumentException("Invalid role: ${user.role}")
        }

        val role = roleRepository.findByName(user.role)
        if (role.isEmpty) {
            throw RoleNotFoundException(name = user.role)
        }
        newUser.addRole(role.get())

        try {
            val savedUser = userRepository.save(newUser)
            val authenticationToken = UsernamePasswordAuthenticationToken(
                user.email,
                user.password
            )

            authenticationManager.authenticate(authenticationToken)
            val userDetails = userDetailsService.loadUserByUsername(user.email)

            val customClaims = mapOf<String, Any>(
                "id" to savedUser.id!!,
                "email" to savedUser.email,
            )

            val token = jwtService.generateToken(customClaims, userDetails)

            return AuthResponse(
                token = token,
                email = savedUser.email
            )
        } catch (e: Exception) {
            logger.error(e.message)
            throw e
        }
    }

    override fun login(loginRequest: LoginRequest): AuthResponse {
        try {
            val authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(
                    loginRequest.email,
                    loginRequest.password
                )
            )

            val userDetails = authentication.principal as org.springframework.security.core.userdetails.UserDetails

            val user = userRepository.findByEmail(loginRequest.email)
                .orElseThrow { UserNotFoundException("User not found") }

            val customClaims = mapOf(
                "id" to user.id!!,
                "email" to user.email,
                "roles" to userDetails.authorities.map { it.authority }
            )

            val token = jwtService.generateToken(customClaims, userDetails)

            return AuthResponse(token = token, email = user.email)

        } catch (ex: AuthenticationException) {
            logger.error("Authentication failed for ${loginRequest.email}",ex)
            throw InvalidCredentialsException("Invalid email or password")
        }
    }
}