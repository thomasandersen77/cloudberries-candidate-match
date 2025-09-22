package no.cloudberries.candidatematch.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**") // Apply CORS configuration to all endpoints under /api
            .allowedOrigins(
                "http://192.168.0.12:5174",
                "http://localhost:5174",
                "http://169.254.189.144:5174"
            ) // Allow requests from your frontend's origin(s)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Specify which methods are allowed
            .allowedHeaders("*") // Allow all headers
            .allowCredentials(true) // Allow cookies and credentials

        // Add CORS configuration for Spring Boot Actuator endpoints
        registry.addMapping("/actuator/**")
            .allowedOrigins(
                "http://192.168.0.12:5174",
                "http://localhost:5174",
                "http://169.254.189.144:5174"
            )
            .allowedMethods(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "OPTIONS"
            )
            .allowedHeaders("*")
            .allowCredentials(true)
    }
}