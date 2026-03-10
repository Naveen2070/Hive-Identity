package com.thehiveproject.identity_service.user.service

import com.thehiveproject.identity_service.user.entity.User
import com.thehiveproject.identity_service.user.exception.RoleNotFoundException
import com.thehiveproject.identity_service.user.repository.RoleRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class UserFactory(
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun createUser(
        email: String,
        passwordRaw: String,
        fullName: String,
        domainRoles: Map<String, String>
    ): User {
        val newUser = User(
            email = email,
            passwordHash = passwordEncoder.encode(passwordRaw),
            fullName = fullName
        )

        domainRoles.forEach { (domain, roleName) ->
            val role = roleRepository.findByName(roleName)
                .orElseThrow { RoleNotFoundException(name = roleName) }
            newUser.addRole(role, domain)
        }
        return newUser
    }
}