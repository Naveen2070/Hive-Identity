package com.thehiveproject.identity_service.user

import com.thehiveproject.identity_service.common.BaseEntity
import com.thehiveproject.identity_service.common.TsidFactory
import jakarta.persistence.*

@Entity
@Table(
    name = "user_roles",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "role_id"])]
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
    var role: Role

) : BaseEntity() {

    // Generate ID automatically
    @PrePersist
    fun generateId() {
        if (this.id == null) {
            this.id = TsidFactory.fastGenerate()
        }
    }
}