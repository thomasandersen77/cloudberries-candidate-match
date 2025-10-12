package no.cloudberries.candidatematch.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig {

    @Bean
    fun corsConfigurationSource(
        @Value("\${cors.allowed-origins:*}") origins: String
    ): CorsConfigurationSource {
        val cors = CorsConfiguration().apply {
            allowedOriginPatterns = origins.split(',').map { it.trim() }
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD")
            allowedHeaders = listOf("*")
            exposedHeaders = listOf("Authorization", "Location", "X-Total-Count")
            allowCredentials = true
            maxAge = 3600
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", cors)
        }
    }
}