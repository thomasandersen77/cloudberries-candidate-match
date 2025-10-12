package no.cloudberries.candidatematch.controllers.auth

import mu.KotlinLogging
import no.cloudberries.candidatematch.infrastructure.repositories.auth.AppUserRepository
import no.cloudberries.candidatematch.config.JwtHelper
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
class AuthController(
    private val users: AppUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwt: JwtHelper
) {
    private val logger = KotlinLogging.logger { }

    data class LoginRequest(val username: String, val password: String)
    data class LoginResponse(val token: String)

    @PostMapping("/login")
    fun login(@RequestBody body: LoginRequest): ResponseEntity<Any> {
        logger.info { "Login attempt from ${body.username}" }
        val user = users.findByUsername(body.username)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "invalid_credentials"))
        if (!passwordEncoder.matches(body.password, user.passwordHash)) {
            logger.warn { "Invalid credentials for user ${body.username}" }
            return ResponseEntity.status(401).body(mapOf("error" to "invalid_credentials"))
        }
        val token = jwt.generateToken(user.username)
        logger.info { "Login attempt from ${body.username} successful" }
        return ResponseEntity.ok(LoginResponse(token))
    }

    @PostMapping("/demo")
    fun demoLoginNoDB(): ResponseEntity<Any> {
        logger.info { "Login attempt from user demo. Token: ${jwt.generateToken("demo")}" }
        val token = jwt.generateToken("demo")
        logger.info { "Login attempt from $token successful" }
        return ResponseEntity.ok(LoginResponse(token))
    }

}