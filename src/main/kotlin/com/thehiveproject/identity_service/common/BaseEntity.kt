package com.thehiveproject.identity_service.common

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
    @Column(name = "version")
    var version: Long = 0,

    @Column(name = "is_active", nullable = false)
    private var isActive: Boolean = true,

    @Column(name = "is_deleted", nullable = false)
    private var isDeleted: Boolean = false,

    @Column(name = "deleted_at")
    private var deletedAt: Instant? = null,
) {

    fun isActive(): Boolean = isActive && !isDeleted

    fun isDeleted(): Boolean = isDeleted

    fun isInactive(): Boolean = !isActive

    protected fun activate() {
        if (!isDeleted) {
            isActive = true
        }
    }

    protected fun deactivate() {
        if (!isDeleted) {
            isActive = false
        }
    }

    protected fun softDelete() {
        if (!isDeleted) {
            isDeleted = true
            isActive = false
            deletedAt = Instant.now()
        }
    }

    protected fun restore() {
        if (isDeleted) {
            isDeleted = false
            isActive = true
            deletedAt = null
        }
    }
}