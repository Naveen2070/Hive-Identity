package com.thehiveproject.identity_service.user

import com.thehiveproject.identity_service.auth.exception.InvalidPasswordException
import com.thehiveproject.identity_service.user.dto.ChangePasswordRequest
import com.thehiveproject.identity_service.user.dto.UpdateProfileRequest
import com.thehiveproject.identity_service.user.dto.UserResponse
import com.thehiveproject.identity_service.user.exception.UserAlreadyDeactivatedException
import com.thehiveproject.identity_service.user.exception.UserAlreadyDeletedException
import com.thehiveproject.identity_service.user.exception.UserNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : UserService {

    @Transactional(readOnly = true)
    override fun getUserProfile(email: String): UserResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { UserNotFoundException("User not found") }

        return UserResponse.fromEntity(user)
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
}