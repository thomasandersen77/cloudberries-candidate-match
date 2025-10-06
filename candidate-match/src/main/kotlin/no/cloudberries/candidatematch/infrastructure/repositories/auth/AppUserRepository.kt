package no.cloudberries.candidatematch.infrastructure.repositories.auth

import no.cloudberries.candidatematch.infrastructure.entities.auth.AppUserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AppUserRepository : JpaRepository<AppUserEntity, Long> {
    fun findByUsername(username: String): AppUserEntity?
    fun existsByUsername(username: String): Boolean
}