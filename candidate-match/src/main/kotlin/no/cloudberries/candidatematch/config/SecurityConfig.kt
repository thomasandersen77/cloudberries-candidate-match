package no.cloudberries.candidatematch.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter
) {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val cors = CorsConfiguration().apply {
            allowedOrigins = listOf(
                "https://delightful-meadow-056d48003.1.azurestaticapps.net",
                "https://cloudberries-candidate-match-ca.whitesand-767916af.westeurope.azurecontainerapps.io",
                "http://localhost:5173",
                "http://localhost:5174"
            )
            allowedMethods = listOf(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS",
                "HEAD"
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
            allowCredentials = false
            maxAge = 3600
        }
        cors.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration(
                "/**",
                cors
            )
        }
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        AntPathRequestMatcher("/"),
                        AntPathRequestMatcher("/auth/login"),
                        AntPathRequestMatcher("/auth/demo"),
                        AntPathRequestMatcher("/health"),
                        AntPathRequestMatcher("/actuator/health"),
                        AntPathRequestMatcher("/actuator/health/**"),
                        AntPathRequestMatcher("/actuator/info"),
                        AntPathRequestMatcher("/v3/api-docs/**"),
                        AntPathRequestMatcher("/swagger-ui/**"),
                        AntPathRequestMatcher("/swagger-ui.html"),
                        // API-prefixed public mirrors
                        AntPathRequestMatcher("/api/auth/login"),
                        AntPathRequestMatcher("/api/auth/demo"),
                        AntPathRequestMatcher("/api/health"),
                        AntPathRequestMatcher("/api/actuator/health"),
                        AntPathRequestMatcher("/api/actuator/health/**"),
                        AntPathRequestMatcher("/api/actuator/info"),
                        AntPathRequestMatcher("/api/v3/api-docs/**"),
                        AntPathRequestMatcher("/api/swagger-ui/**"),
                        AntPathRequestMatcher("/api/swagger-ui.html")
                    ).permitAll()
                    // Keep OPTIONS open for CORS preflight
                    .requestMatchers(AntPathRequestMatcher("/**", HttpMethod.OPTIONS.name())).permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
