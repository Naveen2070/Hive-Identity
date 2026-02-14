package com.thehiveproject.identity_service.auth

import com.thehiveproject.identity_service.auth.dto.AuthResponse
import com.thehiveproject.identity_service.auth.dto.LoginRequest
import com.thehiveproject.identity_service.auth.dto.RegisterRequest
import com.thehiveproject.identity_service.auth.dto.TokenRefreshRequest
import com.thehiveproject.identity_service.auth.exception.InvalidCredentialsException
import com.thehiveproject.identity_service.auth.security.CustomUserDetails
import com.thehiveproject.identity_service.user.RoleRepository
import com.thehiveproject.identity_service.user.User
import com.thehiveproject.identity_service.user.UserRepository
import com.thehiveproject.identity_service.user.exception.RoleNotFoundException
import com.thehiveproject.identity_service.user.exception.UserAlreadyExistsException
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val ALLOWED_SIGNUP_ROLES = "USER,ORGANIZER"

@Service
class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val authenticationManager: AuthenticationManager,
    private val jwtService: JwtService,
    private val refreshTokenService: RefreshTokenService,
    private val passwordEncoder: PasswordEncoder
) : AuthService {

    private val logger = LoggerFactory.getLogger(AuthServiceImpl::class.java)


    @Transactional
    override fun registerUser(user: RegisterRequest): AuthResponse {
        // 1. Check if user exists (Query 1)
        if (userRepository.findByEmail(user.email).isPresent) {
            throw UserAlreadyExistsException("User already exists")
        }

        // 2. Validate Role
        val allowedRoles = ALLOWED_SIGNUP_ROLES.split(",").toSet()
        if (!allowedRoles.contains(user.role)) {
            throw IllegalArgumentException("Invalid role: ${user.role}")
        }

        // 3. Fetch Role (Query 2)
        val role = roleRepository.findByName(user.role)
            .orElseThrow { RoleNotFoundException(name = user.role) }

        // 4. Create User Object
        val newUser = User(
            email = user.email,
            passwordHash = passwordEncoder.encode(user.password),
            fullName = user.fullName,
            domainAccess = user.domainAccess
        )
        newUser.addRole(role)

        try {
            val savedUser = userRepository.save(newUser)

            val authorities = savedUser.roles.map {
                SimpleGrantedAuthority("ROLE_${it.role.name}")
            }

            val userDetails = CustomUserDetails(
                savedUser.id!!,
                savedUser.email,
                savedUser.passwordHash,
                savedUser.isEnabled(),
                accountNonExpired = true,
                credentialsNonExpired = true,
                accountNonLocked = true,
                authorities = authorities
            )

            val customClaims = mapOf(
                "id" to savedUser.id!!,
                "email" to savedUser.email,
                "roles" to savedUser.roles.map { "ROLE_${it.role.name}" }
            )

            val token = jwtService.generateToken(customClaims, userDetails)
            val refreshToken = refreshTokenService.createRefreshToken(savedUser.id!!)
            return AuthResponse(
                token = token,
                refreshToken = refreshToken,
                email = savedUser.email
            )

        } catch (e: Exception) {
            logger.error(e.message)
            throw e
        }
    }

    @Transactional
    override fun login(loginRequest: LoginRequest): AuthResponse {
        try {
            val authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(
                    loginRequest.email,
                    loginRequest.password
                )
            )

            val userDetails = authentication.principal as CustomUserDetails

            val customClaims = mapOf(
                "id" to userDetails.id,
                "email" to userDetails.username,
                "roles" to userDetails.authorities.map { it.authority }
            )

            val token = jwtService.generateToken(customClaims, userDetails)
            val refreshToken = refreshTokenService.createRefreshToken(userDetails.id)

            return AuthResponse(token = token, email = userDetails.username, refreshToken = refreshToken)

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

        val customClaims = mapOf(
            "id" to user.id!!,
            "email" to user.email,
            "roles" to authorities.map { it.authority }
        )

        val newAccessToken = jwtService.generateToken(customClaims, userDetails)

        return AuthResponse(
            token = newAccessToken,
            refreshToken = request.refreshToken,
            email = user.email
        )
    }
}