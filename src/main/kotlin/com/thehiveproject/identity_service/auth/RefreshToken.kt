package com.thehiveproject.identity_service.auth

import com.thehiveproject.identity_service.common.BaseEntity
import com.thehiveproject.identity_service.common.TsidFactory
import com.thehiveproject.identity_service.user.User
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.Instant

@Entity
@Table(
    name = "refresh_tokens", uniqueConstraints = [UniqueConstraint(
        name = "uq_refresh_token",
        columnNames = ["token"]
    )]
)
class RefreshToken(
    @Id
    @Column(name = "id", nullable = false)
    var id: Long? = null,

    @param:NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null,

    @param:Size(max = 255)
    @param:NotNull
    @Column(name = "token", nullable = false)
    var token: String? = null,

    @param:NotNull
    @Column(name = "expiry_date", nullable = false)
    var expiryDate: Instant
) : BaseEntity() {

    @PrePersist
    fun generateId() {
        if (this.id == null) {
            this.id = TsidFactory.fastGenerate()
        }
    }
}