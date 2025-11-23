package no.cloudberries.candidatematch.config

import no.cloudberries.candidatematch.domain.ai.AIProvider
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableConfigurationProperties(
    AISettings::class
)
class AIConfig

@ConfigurationProperties(prefix = "ai")
data class AISettings(
    val enabled: Boolean = true,
    val timeout: Duration = Duration.ofSeconds(30),
    val provider: AIProvider = AIProvider.OPENAI,
    val fallbackEnabled: Boolean = true
)
