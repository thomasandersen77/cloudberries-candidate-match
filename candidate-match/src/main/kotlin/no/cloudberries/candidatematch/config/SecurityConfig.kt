package no.cloudberries.candidatematch.config

import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@Profile("!local")
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter
) {

    companion object {
        private val PUBLIC_ANY = arrayOf(
            "/",
            "/health",
            "/actuator/health/**",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/auth/login",
            "/auth/demo"
        )
        private val PUBLIC_POST = arrayOf(
            "/consultants/search/**",
            "/consultants/cv-score/**",
            "/analytics/**",
            "/chat/**"
        )
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val cors = CorsConfiguration().apply {
            // Allowed origins must be explicit when allowCredentials is true
            allowedOrigins = listOf(
                "https://delightful-meadow-056d48003.1.azurestaticapps.net",
                "https://cloudberries-candidate-match-ca.whitesand-767916af.westeurope.azurecontainerapps.io",
                "http://localhost:5174",
                "http://localhost:5173"
            )
            allowedMethods = listOf(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
            )
            allowedHeaders = listOf(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "Cache-Control",
                "Pragma"
            )
            exposedHeaders = listOf(
                "Authorization",
                "Location"
            )
            allowCredentials = true
            maxAge = 3600
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration(
                "/**",
                cors
            )
        }
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                // TEMP: open all endpoints in Azure/prod to resolve 403s
                auth.anyRequest().permitAll()
            }
            .exceptionHandling {
                it.authenticationEntryPoint(AuthenticationEntryPoint { request, response, authException ->
                    response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Unauthorized"
                    )
                })
            }.exceptionHandling {
                it.accessDeniedHandler { _, response,   _ ->
                    response.status = HttpStatus.FORBIDDEN.value()
                }
            }
            .addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter::class.java
            )
        return http.build()
    }
}
