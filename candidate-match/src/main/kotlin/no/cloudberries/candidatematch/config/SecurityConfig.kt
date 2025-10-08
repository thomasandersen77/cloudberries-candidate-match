import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.util.List

@Bean
fun corsConfigurationSource(): CorsConfigurationSource {
    val cors = CorsConfiguration().apply {
        allowedOrigins = listOf(
            "https://delightful-meadow-056d48003.1.azurestaticapps.net", // SWA prod
            "http://localhost:5174"                                       // Vite dev
        )
        allowedMethods = listOf("GET","POST","PUT","DELETE","PATCH","OPTIONS","HEAD")
        allowedHeaders = listOf("Authorization","Content-Type","Accept")
        allowCredentials = false
        maxAge = 3600
    }
    return UrlBasedCorsConfigurationSource().apply {
        registerCorsConfiguration("/**", cors)
    }
}

@Bean
fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
    http
        .csrf { it.disable() }
        .cors { it.configurationSource(corsConfigurationSource()) }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests { auth ->
            auth
                .requestMatchers("/api/auth/login", "/api/health").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated()
        }
    // .addFilterBefore(JwtAuthFilter(jwtHelper()), UsernamePasswordAuthenticationFilter::class.java)
    return http.build()
}