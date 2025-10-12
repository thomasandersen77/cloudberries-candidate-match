package no.cloudberries.candidatematch.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtHelper: JwtHelper
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Pass CORS preflight immediately
        if (request.method.equals("OPTIONS", ignoreCase = true)) {
            filterChain.doFilter(request, response)
            return
        }

        val authHeader = request.getHeader("Authorization")
        if (authHeader.isNullOrBlank() || !authHeader.startsWith("Bearer ", ignoreCase = true)) {
            // No bearer token -> do not authenticate here. Let SecurityConfig rules decide.
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.substringAfter("Bearer ").trim()
        try {
            val username = JwtUtils.parseSubject(jwtHelper, token)
            if (username != null && SecurityContextHolder.getContext().authentication == null) {
                val auth = UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_USER"), SimpleGrantedAuthority("ROLE_ADMIN"))
                )
                SecurityContextHolder.getContext().authentication = auth
            } else if (username == null) {
                // Invalid token -> clear context but DO NOT send 401/403 here
                SecurityContextHolder.clearContext()
            }
        } catch (ex: Exception) {
            log.debug("JWT parse/validate failed: ${ex.message}")
            SecurityContextHolder.clearContext()
            // Keep passing through; protected endpoints will fail with 401 by entrypoint
        }
        filterChain.doFilter(request, response)
    }
}

object JwtUtils {
    fun parseSubject(jwtHelper: JwtHelper, token: String): String? = try {
        val keyField = jwtHelper.javaClass.getDeclaredField("key").apply { isAccessible = true }
        val key = keyField.get(jwtHelper) as java.security.Key
        val parser = io.jsonwebtoken.Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
        parser.parseClaimsJws(token).body.subject
    } catch (_: Exception) {
        null
    }
}
