package no.cloudberries.candidatematch.config

import no.cloudberries.candidatematch.domain.ai.AIProvider
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "projectrequest")
class ProjectRequestConfig {
    val analysis = AnalysisConfig()

    class AnalysisConfig {
        var aiEnabled: Boolean = true
        var provider: AIProvider = AIProvider.GEMINI
    }
}