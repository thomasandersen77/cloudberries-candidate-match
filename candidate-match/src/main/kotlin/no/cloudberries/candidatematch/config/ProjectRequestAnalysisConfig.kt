package no.cloudberries.candidatematch.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "projectrequest.analysis")
class ProjectRequestAnalysisConfig {
    var aiEnabled: Boolean = true
}
