package com.thehiveproject.identity_service.user.entity

import com.thehiveproject.identity_service.common.entity.BaseEntity
import com.thehiveproject.identity_service.common.utils.TsidFactory
import jakarta.persistence.*

@Entity
@Table(
    name = "user_roles",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "role_id", "domain"])]
)
class UserRole(

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    var role: Role,

    @Column(name = "domain", nullable = false, length = 50)
    var domain: String

) : BaseEntity() {

    // Generate ID automatically
    @PrePersist
    fun generateId() {
        if (this.id == null) {
            this.id = TsidFactory.fastGenerate()
        }
    }
}