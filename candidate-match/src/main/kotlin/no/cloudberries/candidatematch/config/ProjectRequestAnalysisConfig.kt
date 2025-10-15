package no.cloudberries.candidatematch.config

import no.cloudberries.candidatematch.domain.ai.AIProvider
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "projectrequest.analysis")
data class ProjectRequestAnalysisConfig(
    var aiEnabled: Boolean = true,
    var provider: AIProvider = AIProvider.GEMINI
)
