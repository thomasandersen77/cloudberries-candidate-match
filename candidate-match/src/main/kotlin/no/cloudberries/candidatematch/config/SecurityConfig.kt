package no.cloudberries.candidatematch.config

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import no.cloudberries.candidatematch.infrastructure.repositories.auth.AppUserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.stereotype.Component
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import org.springframework.web.filter.OncePerRequestFilter
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey

@Configuration
class SecurityConfig(
    @Value("\${security.jwt.secret:dev-secret-change-me}") private val jwtSecret: String,
    @Value("\${security.jwt.expiration-minutes:480}") private val expirationMinutes: Long,
) {
    private val logger = KotlinLogging.logger { }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun userDetailsService(appUserRepository: AppUserRepository): UserDetailsService = UserDetailsService { username ->
        val user = appUserRepository.findByUsername(username)
            ?: throw org.springframework.security.core.userdetails.UsernameNotFoundException("User not found")
        val roles = user.roles?.split(',')?.filter { it.isNotBlank() }?.map { SimpleGrantedAuthority(it.trim()) } ?: emptyList()
        User(user.username, user.passwordHash, roles)
    }

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager = config.authenticationManager

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val jwtFilter = JwtAuthFilter(jwtHelper())

        http
            .csrf { it.disable() }
            .cors { }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/api/auth/login",
                        "/api/health",
                        "/actuator/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/openapi.yaml",
                        "/openapi.yml"
                    ).permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    @Bean
    fun corsFilter(): CorsFilter {
        val config = CorsConfiguration()
        config.allowCredentials = true
        config.addAllowedOriginPattern("*")
        config.addAllowedHeader("*")
        config.addAllowedMethod("*")

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return CorsFilter(source)
    }

    @Bean
    fun jwtHelper(): JwtHelper = JwtHelper(jwtSecret, expirationMinutes)
}

@Component
class JwtHelper(secret: String, private val expirationMinutes: Long) {
    private val logger = KotlinLogging.logger { }
    private val key: SecretKey = run {
        val raw = secret.toByteArray(StandardCharsets.UTF_8)
        val min = 32 // 256 bits
        val material = if (raw.size < min) {
            val padded = ByteArray(min)
            System.arraycopy(raw, 0, padded, 0, raw.size)
            for (i in raw.size until min) padded[i] = '0'.code.toByte()
            padded
        } else raw
        Keys.hmacShaKeyFor(material)
    }

    fun generateToken(username: String): String {
        val now = Date()
        val expiry = Date(now.time + expirationMinutes * 60_000)
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun parseUsername(token: String): String? {
        return try {
            val claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token)
            claims.body.subject
        } catch (e: Exception) {
            logger.debug(e) { "Invalid JWT" }
            null
        }
    }
}

class JwtAuthFilter(private val helper: JwtHelper) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (header != null && header.startsWith("Bearer ")) {
            val token = header.substring(7)
            val username = helper.parseUsername(token)
            if (!username.isNullOrBlank()) {
                val auth: Authentication = UsernamePasswordAuthenticationToken(username, null, emptyList())
                SecurityContextHolder.getContext().authentication = auth
            }
        }
        filterChain.doFilter(request, response)
    }
}
