package com.thehiveproject.identity_service.user.entity

import com.thehiveproject.identity_service.common.entity.BaseEntity
import com.thehiveproject.identity_service.common.utils.TsidFactory
import jakarta.persistence.*

@Entity
@Table(name = "app_users")
class User(

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @Column(name = "full_name", nullable = false, length = 100)
    var fullName: String,

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    var roles: MutableSet<UserRole> = mutableSetOf()

) : BaseEntity() {

    @PrePersist
    fun generateId() {
        if (this.id == null) {
            this.id = TsidFactory.fastGenerate()
        }
    }

    fun addRole(role: Role, domain: String) {
        val userRole = UserRole(
            user = this,
            role = role,
            domain = domain
        )
        this.roles.add(userRole)
    }

    fun removeRole(role: Role, domain: String) {
        this.roles.removeIf { it.role.id == role.id && it.domain == domain }
    }

    fun activateUser() {
        this.activate()
    }

    fun deactivateUser() {
        this.deactivate()
    }

    fun softDeleteUser() {
        this.softDelete()
    }

    fun restoreUser() {
        this.restore()
    }
}