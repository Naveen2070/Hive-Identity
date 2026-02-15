package com.thehiveproject.identity_service.user.entity

import com.thehiveproject.identity_service.common.entity.BaseEntity
import com.thehiveproject.identity_service.common.utils.TsidFactory
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "domain_access", columnDefinition = "jsonb")
    var domainAccess: MutableSet<String> = mutableSetOf("events"),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    var roles: MutableSet<UserRole> = mutableSetOf()

) : BaseEntity() {

    @PrePersist
    fun generateId() {
        if (this.id == null) {
            this.id = TsidFactory.fastGenerate()
        }
    }

    fun addRole(role: Role) {
        val userRole = UserRole(
            user = this,
            role = role
        )
        this.roles.add(userRole)
    }

    fun removeRole(role: Role) {
        this.roles.removeIf { it.role.id == role.id }
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