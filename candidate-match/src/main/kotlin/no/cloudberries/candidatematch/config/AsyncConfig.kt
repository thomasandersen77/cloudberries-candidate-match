package no.cloudberries.candidatematch.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync

/**
 * Enables Spring's @Async annotation for asynchronous method execution.
 * 
 * Used for background processing of project request matching to avoid
 * blocking HTTP request threads while Gemini API processes rankings.
 */
@Configuration
@EnableAsync
class AsyncConfig
