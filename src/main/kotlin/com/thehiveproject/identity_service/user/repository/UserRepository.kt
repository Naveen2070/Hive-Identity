package com.thehiveproject.identity_service.user.repository

import com.thehiveproject.identity_service.user.dto.UserSummary
import com.thehiveproject.identity_service.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import java.util.*

interface UserRepository : JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    fun findByEmail(email: String): Optional<User>

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.active = true AND u.deleted = false")
    fun findActiveUser(email: String): Optional<User>

    @Query(
        """
    SELECT new com.thehiveproject.identity_service.user.dto.UserSummary(
        u.id,
        u.fullName,
        u.email
    )
    FROM User u
    WHERE u.id = :id
    """
    )
    fun findSummaryById(id: Long): Optional<UserSummary>

    fun findByIdIn(ids: List<Long>): List<UserSummary>
}