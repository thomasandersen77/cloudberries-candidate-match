package no.cloudberries.candidatematch.infrastructure.entities.auth

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "app_user", indexes = [
    Index(name = "ux_app_user_username", columnList = "username", unique = true)
])
class AppUserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true, length = 100)
    var username: String = "",

    @Column(name = "password_hash", nullable = false, length = 100)
    var passwordHash: String = "",

    @Column(name = "roles", nullable = true, length = 200)
    var roles: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)