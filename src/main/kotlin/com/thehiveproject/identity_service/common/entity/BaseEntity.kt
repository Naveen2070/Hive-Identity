package com.thehiveproject.identity_service.common.entity

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity(
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    var createdBy: Long? = null,

    @LastModifiedBy
    @Column(name = "updated_by")
    var updatedBy: Long? = null,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0,

    @Column(name = "is_active", nullable = false)
    protected var active: Boolean = true,

    @Column(name = "is_deleted", nullable = false)
    protected var deleted: Boolean = false,

    @Column(name = "deleted_at")
    protected var deletedAt: Instant? = null,
) {

    fun isEnabled(): Boolean = active && !deleted

    fun isDeleted(): Boolean = deleted

    fun isInactive(): Boolean = !active

    fun wasDeletedAt(): Instant? = deletedAt

    protected fun activate() {
        if (!deleted) {
            active = true
        }
    }

    protected fun deactivate() {
        if (!deleted) {
            active = false
        }
    }

    protected fun softDelete() {
        if (!deleted) {
            deleted = true
            active = false
            deletedAt = Instant.now()
        }
    }

    protected fun restore() {
        if (deleted) {
            deleted = false
            active = true
            deletedAt = null
        }
    }

}