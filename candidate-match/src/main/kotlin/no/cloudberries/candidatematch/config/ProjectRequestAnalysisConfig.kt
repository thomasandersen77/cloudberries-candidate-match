package no.cloudberries.candidatematch.config

import no.cloudberries.candidatematch.domain.ai.AIProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
data class ProjectRequestAnalysisConfig(
    @Value("\${projectrequest.analysis.ai-enabled:true}") val aiEnabled: Boolean,
    @Value("\${projectrequest.analysis.provider:GEMINI}") val provider: AIProvider,
)