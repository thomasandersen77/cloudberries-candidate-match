package no.cloudberries.candidatematch.service.matching

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import no.cloudberries.candidatematch.domain.CandidateMatchResponse
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.domain.ai.AIResponse
import no.cloudberries.candidatematch.domain.candidate.ConsultantMatchedEvent
import no.cloudberries.candidatematch.domain.candidate.Skill
import no.cloudberries.candidatematch.domain.event.DomainEventPublisher
import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import no.cloudberries.candidatematch.service.ai.AIAnalysisService
import no.cloudberries.candidatematch.service.ai.AIService
import no.cloudberries.candidatematch.templates.MatchParams
import no.cloudberries.candidatematch.templates.MatchPromptTemplate
import no.cloudberries.candidatematch.templates.renderMatchTemplate
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CandidateMatchingService(
    private val aiAnalysisService: AIAnalysisService,
    private val domainEventPublisher: DomainEventPublisher,
    private val consultantRepository: ConsultantRepository
) : AIService {
    private val logger = KotlinLogging.logger {}
    private val mapper = jacksonObjectMapper()

    @Timed
    override fun matchCandidate(
        aiProvider: AIProvider,
        cv: String,
        request: String,
        consultantName: String
    ): CandidateMatchResponse {
        val prompt = renderMatchTemplate(
            MatchPromptTemplate.template,
            MatchParams(
                cv = cv,
                request = request,
                consultantName = consultantName
            )
        )

        logger.debug { LOG_PROMPT_GENERATED }
        val response = getAiResponse(
            aiProvider,
            prompt
        )
        return processAiResponse(
            response,
            consultantName
        )
    }

    private fun getAiResponse(aiProvider: AIProvider, prompt: String): AIResponse {
        return when (aiProvider) {
            AIProvider.GEMINI -> {
                logger.debug { LOG_USING_GEMINI }
                aiAnalysisService.analyzeContent(
                    content = prompt,
                    AIProvider.GEMINI
                )
            }

            AIProvider.OPENAI -> {
                logger.debug { LOG_USING_OPENAI }
                aiAnalysisService.analyzeContent(
                    content = prompt,
                    AIProvider.OPENAI
                )
            }
        }
    }

    fun processAiResponse(response: AIResponse, consultantName: String): CandidateMatchResponse {
        val matchResponse = mapper.readValue<CandidateMatchResponse>(content = response.content)
        logger.info { "$LOG_MATCH_SUCCESS $consultantName with score: ${matchResponse.totalScore}" }

        publishMatchEvent(
            consultantName,
            matchResponse
        )
        return matchResponse
    }

    private fun publishMatchEvent(
        consultantName: String,
        matchResponse: CandidateMatchResponse
    ) {
        domainEventPublisher.publish(
            ConsultantMatchedEvent(
                consultantName = consultantName,
                matchScore = matchResponse.totalScore,
                matchSummary = matchResponse.summary,
                occurredOn = Instant.now()
            )
        )
    }

    /**
     * Find candidate matches based on required skills.
     * 
     * @param requiredSkills List of skill names to match against
     * @param aiProvider AI provider to use for matching analysis
     * @return List of candidate match responses
     */
    fun findMatchesBySkills(
        requiredSkills: List<String>,
        aiProvider: AIProvider = AIProvider.GEMINI
    ): List<CandidateMatchResponse> {
        val requestText = "Find consultants with skills: " + requiredSkills.joinToString(", ")
        logger.info { "Processing skills-based match request: ${requestText.take(150)}..." }
        
        val normalizedRequiredSkills = requiredSkills.map { it.lowercase().trim() }
        
        // Get all consultants from repository
        val allConsultants = consultantRepository.findAll()
        logger.info { "Found ${allConsultants.size} consultants in database" }
        
        // Filter consultants based on skills
        val matchingConsultants = filterConsultantsBySkills(allConsultants, normalizedRequiredSkills)
        logger.info { "Found ${matchingConsultants.size} consultants matching skill criteria" }
        
        // Process each matching consultant through AI analysis
        return matchingConsultants.map { consultant ->
            logger.info { "Processing consultant: ${consultant.name}" }
            
            val match = matchCandidate(
                aiProvider = aiProvider,
                cv = extractCvText(consultant),
                request = requestText,
                consultantName = consultant.name
            )
            
            logger.info { "Finished processing consultant: ${consultant.name} with score: ${match.totalScore}" }
            match
        }
    }
    
    /**
     * Filter consultants based on their skills matching the required skills.
     * Uses both enum-based skills and text-based skill analysis from resume data.
     */
    private fun filterConsultantsBySkills(
        consultants: List<ConsultantEntity>, 
        requiredSkills: List<String>
    ): List<ConsultantEntity> {
        return consultants.filter { consultant ->
            // Check enum-based skills
            val consultantSkillNames = consultant.skills.map { it.name.lowercase() }
            val hasEnumSkillMatch = requiredSkills.any { requiredSkill ->
                consultantSkillNames.any { consultantSkill ->
                    consultantSkill.contains(requiredSkill) || requiredSkill.contains(consultantSkill)
                }
            }
            
            // Check resume data for skill mentions (fallback)
            val resumeText = consultant.resumeData.toString().lowercase()
            val hasResumeSkillMatch = requiredSkills.any { skill ->
                resumeText.contains(skill)
            }
            
            // Return true if either enum skills or resume contains the required skills
            hasEnumSkillMatch || hasResumeSkillMatch
        }
    }
    
    /**
     * Extract CV text from consultant entity.
     * This handles the resume data format used in the system.
     */
    private fun extractCvText(consultant: ConsultantEntity): String {
        return consultant.resumeData.toString()
    }

    companion object {
        private const val LOG_PROMPT_GENERATED = "Generated prompt for AI analysis"
        private const val LOG_USING_GEMINI = "Using Gemini for analysis"
        private const val LOG_USING_OPENAI = "Using OpenAI for analysis"
        private const val LOG_MATCH_SUCCESS = "Successfully matched candidate"
    }
}