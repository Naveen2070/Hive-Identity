package com.thehiveproject.identity_service.user.entity

import com.thehiveproject.identity_service.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "roles")
class Role(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @Column(unique = true, nullable = false, length = 50)
    var name: String

) : BaseEntity()