package no.cloudberries.candidatematch.infrastructure.web

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * Puts a requestId into the MDC for all logs within the request lifecycle.
 *
 * - Reads X-Request-Id from the incoming request if present, otherwise generates a UUID.
 * - Adds the value to response header X-Request-Id for traceability across services.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestIdFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val headerName = "X-Request-Id"
        val requestId = request.getHeader(headerName)
            ?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()

        try {
            MDC.put("requestId", requestId)
            response.setHeader(headerName, requestId)
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove("requestId")
        }
    }
}