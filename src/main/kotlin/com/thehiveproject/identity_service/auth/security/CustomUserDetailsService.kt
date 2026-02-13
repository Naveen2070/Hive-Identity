package com.thehiveproject.identity_service.auth.security

import com.thehiveproject.identity_service.user.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    @Transactional(readOnly = true)
    override fun loadUserByUsername(email: String): UserDetails {
        // We reuse your existing logic to find by email
        val userEntity = userRepository.findByEmail(email)
            if(userEntity.isEmpty)
            throw UsernameNotFoundException("User not found with username or email: $email")
        val user = userEntity.get()

        // Map Roles to Authorities
        val authorities = user.roles
            .filter { !it.isDeleted() }
            .map { userRole ->
                SimpleGrantedAuthority("ROLE_${userRole.role.name}")
            }.toSet()

        // Return the Spring Security User object
        return CustomUserDetails(
            user.id!!,
            user.email,
            user.passwordHash,
            !user.isInactive(),
            user.isActive(),
            true,
            !user.isDeleted(),
            authorities
        )
    }
}