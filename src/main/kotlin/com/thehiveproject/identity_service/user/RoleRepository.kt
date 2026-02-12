package com.thehiveproject.identity_service.user

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface RoleRepository : JpaRepository<Role, Int> {
    fun findByName(name: String): Optional<Role>
}