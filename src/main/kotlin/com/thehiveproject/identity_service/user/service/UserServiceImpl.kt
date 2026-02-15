package com.thehiveproject.identity_service.user.service

import com.thehiveproject.identity_service.auth.dto.CreateUserRequest
import com.thehiveproject.identity_service.user.dto.UserDto
import com.thehiveproject.identity_service.auth.exception.InvalidPasswordException
import com.thehiveproject.identity_service.auth.service.RefreshTokenService
import com.thehiveproject.identity_service.user.dto.ChangePasswordRequest
import com.thehiveproject.identity_service.user.dto.UpdateProfileRequest
import com.thehiveproject.identity_service.user.dto.UserResponse
import com.thehiveproject.identity_service.user.entity.User
import com.thehiveproject.identity_service.user.exception.*
import com.thehiveproject.identity_service.user.mapper.UserMapper.toDto
import com.thehiveproject.identity_service.user.repository.RoleRepository
import com.thehiveproject.identity_service.user.repository.UserRepository
import com.thehiveproject.identity_service.user.repository.UserSpecification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val refreshTokenService: RefreshTokenService,
    private val roleRepository: RoleRepository
) : UserService {
    override fun createInternalUser(request: CreateUserRequest): UserDto {
        if (userRepository.findByEmail(request.email).isPresent) {
            throw UserAlreadyExistsException("User already exists")
        }

        val role = roleRepository.findByName(request.role)
            .orElseThrow { RoleNotFoundException(request.role) }

        val newUser = User(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
            fullName = request.fullName,
            domainAccess = request.domainAccess
        )
        newUser.addRole(role)

        return userRepository.save(newUser).toDto()
    }

    @Transactional(readOnly = true)
    override fun getUserProfile(email: String): UserResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { UserNotFoundException("User not found") }

        return UserResponse.fromEntity(user)
    }

    @Transactional(readOnly = true)
    override fun getAllUsers(pageable: Pageable, search: String?): Page<UserDto> {
        val spec = UserSpecification.hasSearchQuery(search)

        // Example: If you wanted to filter by deleted status, you just chain it:
        // spec = spec.and(UserSpecification.isDeleted(false))

        return userRepository.findAll(spec, pageable)
            .map { it.toDto() }
    }

    @Transactional(readOnly = true)
    override fun getUserById(id: Long): UserDto {
        val user = userRepository.findById(id)
            .orElseThrow { UserNotFoundException("User not found") }
        return user.toDto()
    }

    @Transactional
    override fun updateProfile(email: String, request: UpdateProfileRequest): UserResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { UserNotFoundException("User not found") }

        request.fullName?.let {
            user.fullName = it
        }


        val savedUser = userRepository.save(user)
        return UserResponse.fromEntity(savedUser)
    }

    @Transactional(readOnly = true)
    override fun changeUserStatus(
        id: Long,
        active: Boolean
    ): UserDto {
        val user = userRepository.findById(id)
            .orElseThrow { UserNotFoundException("User not found") }

        if (active) {
            user.activateUser()
        } else {
            user.deactivateUser()
            refreshTokenService.revokeTokensForUser(id)
        }

        return userRepository.save(user).toDto()
    }

    @Transactional
    override fun changePassword(email: String, request: ChangePasswordRequest) {
        val user = userRepository.findByEmail(email)
            .orElseThrow { UserNotFoundException("User not found") }

        // 1. Verify Old Password
        if (!passwordEncoder.matches(request.oldPassword, user.passwordHash)) {
            throw InvalidPasswordException("Incorrect old password")
        }

        // 2. Hash New Password
        user.passwordHash = passwordEncoder.encode(request.newPassword)

        // 3. Save
        userRepository.save(user)
    }

    @Transactional
    override fun deactivateAccount(email: String) {
        val user = userRepository.findByEmail(email)
            .orElseThrow { UserNotFoundException("User not found") }

        if (user.isDeleted()) {
            throw UserAlreadyDeletedException("Deleted user cannot be deactivated")
        }
        if (user.isInactive()) {
            throw UserAlreadyDeactivatedException("User is already deactivated")
        }

        user.deactivateUser()

        userRepository.save(user)
    }

    @Transactional
    override fun deleteAccount(email: String) {
        val user = userRepository.findByEmail(email)
            .orElseThrow { UserNotFoundException("User not found") }

        if (user.isDeleted()) {
            throw UserAlreadyDeletedException("User is already deleted")
        }
        user.softDeleteUser()

        userRepository.save(user)
    }

    override fun hardDeleteUser(id: Long) {
        if (!userRepository.existsById(id)) {
            throw UserNotFoundException("User not found")
        }

        refreshTokenService.revokeTokensForUser(id)

        userRepository.deleteById(id)
    }
}