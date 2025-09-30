package no.cloudberries.candidatematch.domain.consultant

/**
 * Search criteria for relational (structured) consultant search
 */
data class RelationalSearchCriteria(
    val name: String? = null,
    val skillsAll: List<String> = emptyList(),
    val skillsAny: List<String> = emptyList(),
    val minQualityScore: Int? = null,
    val onlyActiveCv: Boolean = false
) {
    /**
     * Validates the search criteria
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (minQualityScore != null && (minQualityScore < 0 || minQualityScore > 100)) {
            errors.add("minQualityScore must be between 0 and 100")
        }
        
        val allSkills = skillsAll + skillsAny
        if (allSkills.any { it.isBlank() }) {
            errors.add("Skills cannot be blank")
        }
        
        return errors
    }
}

/**
 * Search criteria for semantic consultant search
 */
data class SemanticSearchCriteria(
    val text: String,
    val provider: String = "GOOGLE_GEMINI",
    val model: String = "text-embedding-004",
    val topK: Int = 10,
    val minQualityScore: Int? = null,
    val onlyActiveCv: Boolean = false
) {
    /**
     * Validates the search criteria
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (text.isBlank()) {
            errors.add("Search text cannot be blank")
        }
        
        if (topK <= 0 || topK > 100) {
            errors.add("topK must be between 1 and 100")
        }
        
        if (minQualityScore != null && (minQualityScore < 0 || minQualityScore > 100)) {
            errors.add("minQualityScore must be between 0 and 100")
        }
        
        if (provider.isBlank()) {
            errors.add("Provider cannot be blank")
        }
        
        if (model.isBlank()) {
            errors.add("Model cannot be blank")
        }
        
        return errors
    }
}