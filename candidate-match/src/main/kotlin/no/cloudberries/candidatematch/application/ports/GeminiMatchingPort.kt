package no.cloudberries.candidatematch.application.ports

/**
 * Port interface for Gemini AI matching operations.
 * Follows Clean Architecture by defining the contract in the application layer
 * while implementation resides in the infrastructure layer.
 */
interface GeminiMatchingPort {
    
    /**
     * Ensures that the File Search store exists in Gemini.
     * This operation is idempotent - it's safe to call multiple times.
     * 
     * @return the store name/ID
     */
    suspend fun ensureStore(): String
    
    /**
     * Uploads a CV file to Gemini's File API and adds it to the File Search store.
     * 
     * @param fileId unique identifier for the CV (e.g., consultant ID)
     * @param filename display name for the file
     * @param bytes PDF or text content as byte array
     * @return URI of the uploaded file in Gemini
     */
    suspend fun uploadCv(fileId: String, filename: String, bytes: ByteArray): String
    
    /**
     * Ranks candidates against a project request using Gemini File Search.
     * Uses the managed RAG capability to search through CVs and provide
     * structured ranking with scores and reasons.
     * 
     * @param projectRequestId unique identifier for the project request
     * @param projectDescription formatted text describing the project requirements
     * @param candidates list of candidate snapshots to evaluate
     * @param topN maximum number of candidates to return
     * @return list of ranked candidates with scores and explanations
     */
    suspend fun rankCandidates(
        projectRequestId: String,
        projectDescription: String,
        candidates: List<CandidateSnapshot>,
        topN: Int
    ): List<RankedCandidateDto>
}

/**
 * Snapshot of candidate information for matching.
 * Lightweight DTO to pass to the AI service.
 */
data class CandidateSnapshot(
    val consultantId: String,
    val cvGeminiUri: String?,  // URI of CV in Gemini File Store (null if not uploaded yet)
    val cvQuality: Int,        // CV quality score from 0-100 (from existing evaluation)
    val skills: List<String>,  // List of skill names
    val name: String? = null   // Consultant name for context
)

/**
 * Result of candidate ranking from Gemini.
 * Contains score and structured reasons for the match.
 */
data class RankedCandidateDto(
    val consultantId: String,
    val score: Int,            // Match score from 0-100
    val reasons: List<String>  // List of reasons explaining the match
)
