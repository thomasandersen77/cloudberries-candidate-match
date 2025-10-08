package no.cloudberries.candidatematch.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtHelper: JwtHelper
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            try {
                val username = JwtUtils.parseSubject(jwtHelper, token)
                if (username != null && SecurityContextHolder.getContext().authentication == null) {
                    val auth = UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        listOf(SimpleGrantedAuthority("ROLE_USER"))
                    )
                    SecurityContextHolder.getContext().authentication = auth
                }
            } catch (_: Exception) {
                // Invalid token – leave unauthenticated
            }
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