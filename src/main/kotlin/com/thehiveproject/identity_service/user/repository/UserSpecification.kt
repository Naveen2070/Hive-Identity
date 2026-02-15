package com.thehiveproject.identity_service.user.repository

import com.thehiveproject.identity_service.user.entity.User
import org.springframework.data.jpa.domain.Specification

object UserSpecification {

    fun hasSearchQuery(search: String?): Specification<User> {
        return Specification { root, _, cb ->
            if (search.isNullOrBlank()) {
                cb.conjunction() // Returns "TRUE" (No filtering)
            } else {
                val likePattern = "%${search.lowercase()}%"
                cb.or(
                    cb.like(cb.lower(root.get("email")), likePattern),
                    cb.like(cb.lower(root.get("fullName")), likePattern)
                )
            }
        }
    }

    fun isDeleted(deleted: Boolean?): Specification<User> {
        return Specification { root, _, cb ->
            if (deleted == null) cb.conjunction()
            else cb.equal(root.get<Boolean>("deleted"), deleted)
        }
    }

    fun isActive(active: Boolean?): Specification<User> {
        return Specification { root, _, cb ->
            if (active == null) cb.conjunction()
            else cb.equal(root.get<Boolean>("active"), active)
        }
    }
}