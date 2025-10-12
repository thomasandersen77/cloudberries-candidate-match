package no.cloudberries.candidatematch.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfigurationSource

/**
 * Extremely simple security for local development:
 * - CSRF disabled
 * - Stateless
 * - CORS via properties (CorsConfigurationSource bean)
 * - All requests permitted (avoid 403s while iterating)
 *
 * Activate with: -Dspring-boot.run.profiles=local
 */
@Configuration
@Profile("prod")
class LocalSecurityConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { } // use global CorsConfigurationSource from properties
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/**").permitAll()
            }
        return http.build()
    }
}
