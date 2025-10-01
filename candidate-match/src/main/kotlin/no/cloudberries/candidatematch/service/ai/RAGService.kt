package no.cloudberries.candidatematch.service.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.cloudberries.candidatematch.config.AIChatConfig
import no.cloudberries.candidatematch.domain.ai.AIContentGenerator
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.dto.ai.RAGSource
import no.cloudberries.candidatematch.infrastructure.integration.ai.AIContentGeneratorFactory
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.stereotype.Service
import java.util.*

@Service
class RAGService(
    private val consultantRepository: ConsultantRepository,
    private val aiContentGeneratorFactory: AIContentGeneratorFactory,
    private val config: AIChatConfig,
    private val objectMapper: ObjectMapper,
    private val conversationService: ConversationService,
    private val ragContextService: RAGContextService
) {
    private val logger = KotlinLogging.logger { }

    data class RAGResult(
        val answer: String,
        val sources: List<RAGSource>,
        val conversationId: String
    )

    @Timed
    fun processRAGQuery(
        consultantId: String,
        cvId: String?,
        question: String,
        conversationId: String?
    ): RAGResult {
        logger.info { "Processing RAG query for consultant=$consultantId, cvId=$cvId, question='$question'" }

        // Get or create conversation ID
        val actualConversationId = conversationId ?: UUID.randomUUID().toString()
        
        // Retrieve consultant's CV data
        val consultant = consultantRepository.findByUserId(consultantId)
            ?: throw IllegalArgumentException("Consultant not found: $consultantId")

        val resolvedCvId = cvId ?: consultant.cvId // Use active/default when missing

        // Ensure chunks and retrieve top-k
        val chunks = try {
            ragContextService.ensureChunks(consultantId, resolvedCvId)
            ragContextService.retrieveTopK(consultantId, resolvedCvId, question, config.rag.topKChunks)
        } catch (e: Exception) {
            logger.warn(e) { "RAG chunk pipeline failed for $consultantId/$resolvedCvId; falling back to full CV" }
            emptyList()
        }

        val cvText = if (chunks.isNotEmpty()) {
            "Top context chunks (most relevant first):\n\n" + chunks.joinToString("\n\n---\n\n") { it.text }
        } else {
            formatCvJsonForAI(consultant.resumeData, consultant.name)
        }
        
        // Get conversation history
        val conversationHistory = conversationService.getConversationHistory(actualConversationId)
        
        // Build the prompt with context and conversation history
        val prompt = buildRAGPrompt(question, cvText, consultant.name, conversationHistory)
        
        // Generate AI response
        val provider = AIProvider.valueOf(config.provider)
        val aiGenerator = aiContentGeneratorFactory.getGenerator(provider)
        val aiResponse = aiGenerator.generateContent(prompt)
        
        // Store this exchange in conversation history
        conversationService.addToConversation(actualConversationId, question, aiResponse.content)
        
        // Create source information
        val sources = if (chunks.isNotEmpty()) {
            chunks.map { c ->
                RAGSource(
                    consultantId = generateConsistentUUID(consultantId),
                    consultantName = consultant.name,
                    chunkId = generateConsistentUUID("${consultantId}-${resolvedCvId}-${c.index}"),
                    text = c.text.take(200),
                    score = c.score,
                    location = "CV chunk #${c.index}"
                )
            }
        } else {
            listOf(
                RAGSource(
                    consultantId = generateConsistentUUID(consultantId),
                    consultantName = consultant.name,
                    chunkId = generateConsistentUUID(resolvedCvId),
                    text = "CV data for ${consultant.name}",
                    score = 1.0,
                    location = "Complete CV"
                )
            )
        }

        return RAGResult(
            answer = aiResponse.content,
            sources = sources,
            conversationId = actualConversationId
        )
    }

    private fun formatCvJsonForAI(resumeData: JsonNode, consultantName: String): String {
        return try {
            val cv = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resumeData)
            """
            CV Information for $consultantName:
            
            $cv
            """.trimIndent()
        } catch (e: Exception) {
            logger.warn(e) { "Failed to format CV JSON for consultant $consultantName" }
            "CV data for $consultantName (formatting error)"
        }
    }

    private fun buildRAGPrompt(
        question: String,
        cvText: String,
        consultantName: String,
        conversationHistory: List<ConversationService.ConversationTurn>
    ): String {
        val historyContext = if (conversationHistory.isNotEmpty()) {
            val historyText = conversationHistory.takeLast(5).joinToString("\n\n") { turn ->
                "Human: ${turn.question}\nAssistant: ${turn.answer}"
            }
            "\n\nPrevious conversation context:\n$historyText\n"
        } else {
            ""
        }

        return """
        You are an AI assistant helping to answer questions about a specific consultant based on their CV information.
        
        **Consultant Information:**
        $cvText
        
        **Instructions:**
        - Answer the question based solely on the provided CV information for $consultantName
        - If the information is not available in the CV, state that clearly
        - Be specific and cite relevant details from the CV when possible
        - Keep your answers helpful and professional
        - Always end your response with a relevant follow-up question to keep the conversation engaging and moving forward
        $historyContext
        
        **Question:** $question
        
        **Answer:**
        """.trimIndent()
    }
    
    private fun generateConsistentUUID(input: String): UUID {
        // Generate a consistent UUID based on string input
        return try {
            UUID.fromString(input)
        } catch (e: IllegalArgumentException) {
            // If input is not a valid UUID, create a deterministic one from the hash
            val hash = input.hashCode().toString().padStart(8, '0')
            UUID.fromString("$hash-0000-0000-0000-000000000000")
        }
    }
}
