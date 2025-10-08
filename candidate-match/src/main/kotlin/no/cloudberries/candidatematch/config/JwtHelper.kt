package no.cloudberries.candidatematch.config

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey

/**
 * Minimal JWT helper to generate HMAC-SHA256 tokens for simple auth flows.
 *
 * Secret resolution order:
 * 1) Env var JWT_SECRET
 * 2) Spring property security.jwt.secret
 * 3) Fallback insecure dev secret (only for non-prod convenience)
 */
@Component
class JwtHelper(
    @Value("\${security.jwt.secret:}") private val secretProp: String?
) {
    private val key: SecretKey by lazy {
        val raw = (System.getenv("JWT_SECRET") ?: secretProp ?: DEFAULT_DEV_SECRET)
            .toByteArray(StandardCharsets.UTF_8)
        val normalized = if (raw.size < 32) raw + ByteArray(32 - raw.size) { 0 } else raw
        Keys.hmacShaKeyFor(normalized)
    }

    fun generateToken(username: String, expiresMinutes: Long = 60): String {
        val now = System.currentTimeMillis()
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(Date(now))
            .setExpiration(Date(now + expiresMinutes * 60 * 1000))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    companion object {
        private const val DEFAULT_DEV_SECRET = "change-me-dev-secret-change-me-change-me-32+"
    }
}