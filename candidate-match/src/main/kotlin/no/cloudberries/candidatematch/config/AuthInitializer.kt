package no.cloudberries.candidatematch.config

import mu.KotlinLogging
import no.cloudberries.candidatematch.infrastructure.entities.auth.AppUserEntity
import no.cloudberries.candidatematch.infrastructure.repositories.auth.AppUserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class AuthInitializer {
    private val logger = KotlinLogging.logger { }

    @Bean
    fun ensureDefaultUser(users: AppUserRepository, encoder: PasswordEncoder) = CommandLineRunner {
        if (users.count() == 0L) {
            val defaultUser = AppUserEntity(
                username = "admin",
                passwordHash = encoder.encode("admin123"),
                roles = "ROLE_ADMIN"
            )
            users.save(defaultUser)
            logger.warn { "Created default admin user 'admin' with password 'admin123'. CHANGE THIS IN PRODUCTION!" }
        }
    }
}