package no.cloudberries.ai.config

import no.cloudberries.ai.domain.AIProvider
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.time.Duration

@ConfigurationProperties(prefix = "ai")
class AISettings {
    var enabled: Boolean = true
    var timeout: Duration = Duration.ofSeconds(30)
    var provider: AIProvider = AIProvider.OPENAI
    var fallbackEnabled: Boolean = true
}
