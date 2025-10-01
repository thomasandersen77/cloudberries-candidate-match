package no.cloudberries.candidatematch.service.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import no.cloudberries.candidatematch.config.AIChatConfig
import no.cloudberries.candidatematch.dto.ai.*
import no.cloudberries.candidatematch.infrastructure.integration.gemini.GeminiHttpClient
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import kotlin.math.max

@Service
class AIQueryInterpretationService(
    private val geminiClient: GeminiHttpClient,
    private val config: AIChatConfig,
    private val objectMapper: ObjectMapper
) {
    private val logger = KotlinLogging.logger { }
    
    companion object {
        private val SKILL_SYNONYMS = mapOf(
            // Programming languages
            "javascript" to listOf("js", "node", "nodejs", "node.js"),
            "typescript" to listOf("ts"),
            "kotlin" to listOf("kt"),
            "java" to listOf("jvm"),
            "python" to listOf("py"),
            "csharp" to listOf("c#", ".net", "dotnet"),
            
            // Frameworks
            "spring" to listOf("spring boot", "springboot"),
            "react" to listOf("reactjs", "react.js"),
            "angular" to listOf("angularjs"),
            "vue" to listOf("vuejs", "vue.js"),
            
            // Roles and titles
            "architect" to listOf("architecture", "system architect", "solution architect"),
            "tech lead" to listOf("technical lead", "team lead", "lead developer"),
            "senior" to listOf("sr", "senior developer"),
            "fullstack" to listOf("full-stack", "full stack"),
            "backend" to listOf("back-end", "server-side"),
            "frontend" to listOf("front-end", "client-side"),
            "devops" to listOf("dev ops", "platform engineer")
        )
    }

    @Cacheable(value = ["queryInterpretations"], key = "#userText + #forceMode?.name")
    @Timed
    fun interpretQuery(userText: String, forceMode: SearchMode? = null): QueryInterpretation {
        logger.info { "Interpreting query: '$userText' with forceMode: $forceMode" }
        
        if (forceMode != null) {
            logger.info { "Using forced mode: $forceMode" }
            return createForcedInterpretation(userText, forceMode)
        }
        
        val prompt = createInterpretationPrompt(userText)
        
        try {
            val aiResponse = geminiClient.generateContent(prompt)
            val interpretation = parseInterpretationResponse(aiResponse.content)
            
            logger.info { 
                "Interpretation result: route=${interpretation.route}, " +
                "confidence=${interpretation.confidence.route}" 
            }
            
            return interpretation
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to interpret query, falling back to semantic search" }
            return createFallbackInterpretation(userText)
        }
    }
    
    private fun createInterpretationPrompt(userText: String): String {
        return """
You are an expert at analyzing consultant search queries. Your job is to classify the search intent and extract structured criteria.

Analyze this user query and respond with ONLY a valid JSON object following this exact schema:

{
  "route": "structured|semantic|hybrid|rag",
  "structured": {
    "skillsAll": ["skill1", "skill2"],
    "skillsAny": ["skill3", "skill4"], 
    "roles": ["role1"],
    "minQualityScore": 85,
    "locations": [],
    "availability": null,
    "publicSector": true,
    "customersAny": ["sparebank1", "bank"],
    "industries": ["finance", "public"]
  },
  "semanticText": "search description",
  "consultantName": "name if mentioned",
  "question": "specific question if asking about consultant",
  "confidence": {
    "route": 0.87,
    "extraction": 0.92
  }
}

Classification rules:
1. STRUCTURED: Clear skill requirements (e.g., "find developers who know Java and Spring")
2. SEMANTIC: Descriptive qualities (e.g., "experienced mentor who can guide juniors") 
3. HYBRID: Both specific skills AND descriptive qualities
4. RAG: Asking about a specific consultant by name

Industry & customer extraction:
- Detect if query targets public sector (keywords: kommune, etat, nav, skatt, stat, offentlig, departement, direktorat)
- Detect customer/org hints (e.g., "sparebank1", "bank", "finans", "helse") and set customersAny/industries accordingly
- Keep skills normalized to lowercase

Skills normalization:
- Use lowercase, consistent naming
- "Javascript" → "javascript", "JS" → "javascript"
- "Spring Boot" → "spring", "React.js" → "react"
- "C#" → "csharp", ".NET" → "csharp"

Examples:

Query: "Find consultants who know Kotlin and Spring"
→ {"route":"structured","structured":{"skillsAll":["kotlin","spring"],"skillsAny":[],"roles":[],"minQualityScore":null},"semanticText":null,"consultantName":null,"question":null,"confidence":{"route":0.95,"extraction":0.9}}

Query: "Experienced fullstack developer who can mentor juniors"  
→ {"route":"semantic","structured":null,"semanticText":"experienced fullstack developer who can mentor juniors","consultantName":null,"question":null,"confidence":{"route":0.9,"extraction":0.8}}

Query: "Senior architects with React experience and quality score above 85"
→ {"route":"hybrid","structured":{"skillsAll":[],"skillsAny":["react"],"roles":["senior","architect"],"minQualityScore":85},"semanticText":"senior architects with experience","consultantName":null,"question":null,"confidence":{"route":0.85,"extraction":0.88}}

Query: "Tell me about Thomas Andersen's experience with React"
→ {"route":"rag","structured":null,"semanticText":null,"consultantName":"Thomas Andersen","question":"experience with React","confidence":{"route":0.95,"extraction":0.9}}

Now analyze this query:
"$userText"

Respond with ONLY the JSON object, no other text:
        """.trimIndent()
    }
    
    private fun parseInterpretationResponse(content: String): QueryInterpretation {
        return try {
            // Clean the response in case there are markdown code blocks
            val cleanJson = content
                .replace("```json", "")
                .replace("```", "")
                .trim()
            
            objectMapper.readValue<QueryInterpretationResponse>(cleanJson).toDomain()
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse AI interpretation response: $content" }
            throw IllegalStateException("Failed to parse AI interpretation", e)
        }
    }
    
    private fun createForcedInterpretation(userText: String, mode: SearchMode): QueryInterpretation {
        return when (mode) {
            SearchMode.STRUCTURED -> {
                val skills = extractBasicSkills(userText)
                QueryInterpretation(
                    route = SearchMode.STRUCTURED,
                    structured = StructuredCriteria(skillsAll = skills),
                    semanticText = null,
                    consultantName = null,
                    question = null,
                    confidence = ConfidenceScores(route = 1.0, extraction = 0.7)
                )
            }
            SearchMode.SEMANTIC -> QueryInterpretation(
                route = SearchMode.SEMANTIC,
                structured = null,
                semanticText = userText,
                consultantName = null,
                question = null,
                confidence = ConfidenceScores(route = 1.0, extraction = 0.8)
            )
            SearchMode.HYBRID -> {
                val skills = extractBasicSkills(userText)
                QueryInterpretation(
                    route = SearchMode.HYBRID,
                    structured = StructuredCriteria(skillsAny = skills),
                    semanticText = userText,
                    consultantName = null,
                    question = null,
                    confidence = ConfidenceScores(route = 1.0, extraction = 0.75)
                )
            }
            SearchMode.RAG -> QueryInterpretation(
                route = SearchMode.RAG,
                structured = null,
                semanticText = null,
                consultantName = extractConsultantName(userText),
                question = userText,
                confidence = ConfidenceScores(route = 1.0, extraction = 0.6)
            )
        }
    }
    
    private fun createFallbackInterpretation(userText: String): QueryInterpretation {
        return QueryInterpretation(
            route = SearchMode.SEMANTIC,
            structured = null,
            semanticText = userText,
            consultantName = null,
            question = null,
            confidence = ConfidenceScores(route = 0.5, extraction = 0.5)
        )
    }
    
    private fun extractBasicSkills(text: String): List<String> {
        val words = text.lowercase().split(Regex("\\s+|,|\\.|;"))
        val skills = mutableSetOf<String>()
        
        // Look for known skills and their synonyms
        for ((canonical, synonyms) in SKILL_SYNONYMS) {
            if (words.any { word -> 
                word == canonical || synonyms.any { synonym -> 
                    word.contains(synonym, ignoreCase = true) 
                }
            }) {
                skills.add(canonical)
            }
        }
        
        return skills.toList()
    }
    
    private fun extractConsultantName(text: String): String? {
        // Simple name extraction - look for capitalized words that might be names
        val words = text.split(Regex("\\s+"))
        val namePattern = Regex("[A-Z][a-z]+\\s+[A-Z][a-z]+")
        val match = namePattern.find(text)
        return match?.value
    }
}

/**
 * Internal DTO for parsing AI responses
 */
private data class QueryInterpretationResponse(
    val route: String,
    val structured: StructuredCriteriaResponse?,
    val semanticText: String?,
    val consultantName: String?,
    val question: String?,
    val confidence: ConfidenceScoresResponse
) {
    fun toDomain(): QueryInterpretation {
        return QueryInterpretation(
            route = SearchMode.valueOf(route.uppercase()),
            structured = structured?.toDomain(),
            semanticText = semanticText,
            consultantName = consultantName,
            question = question,
            confidence = confidence.toDomain()
        )
    }
}

private data class StructuredCriteriaResponse(
    val skillsAll: List<String>?,
    val skillsAny: List<String>?,
    val roles: List<String>?,
    val minQualityScore: Int?,
    val locations: List<String>?,
    val availability: String?,
    val publicSector: Boolean?,
    val customersAny: List<String>?,
    val industries: List<String>?
) {
    fun toDomain(): StructuredCriteria {
        return StructuredCriteria(
            skillsAll = skillsAll ?: emptyList(),
            skillsAny = skillsAny ?: emptyList(),
            roles = roles ?: emptyList(),
            minQualityScore = minQualityScore,
            locations = locations ?: emptyList(),
            availability = availability,
            publicSector = publicSector,
            customersAny = customersAny ?: emptyList(),
            industries = industries ?: emptyList()
        )
    }
}

private data class ConfidenceScoresResponse(
    val route: Double,
    val extraction: Double
) {
    fun toDomain(): ConfidenceScores {
        return ConfidenceScores(
            route = max(0.0, kotlin.math.min(1.0, route)),
            extraction = max(0.0, kotlin.math.min(1.0, extraction))
        )
    }
}