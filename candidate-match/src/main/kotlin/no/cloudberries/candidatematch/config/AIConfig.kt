package no.cloudberries.candidatematch.config

import no.cloudberries.ai.config.AISettings
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(
    AISettings::class
)
class AIConfig
