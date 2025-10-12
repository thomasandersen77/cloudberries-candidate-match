package no.cloudberries.candidatematch.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter


@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class ApiPrefixForwardFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val uri = request.requestURI ?: ""
        if (uri.startsWith("/api/")) {
            val target = uri.removePrefix("/api") + (request.queryString?.let { "?" + it } ?: "")
            val dispatcher = request.getRequestDispatcher(target)
            dispatcher.forward(request, response)
            return
        }
        filterChain.doFilter(request, response)
    }
}