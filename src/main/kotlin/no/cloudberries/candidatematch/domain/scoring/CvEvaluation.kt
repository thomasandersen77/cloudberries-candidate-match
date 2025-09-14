package no.cloudberries.candidatematch.domain.scoring

data class ScoreBreakdown(
    val structureAndReadability: CriterionScore?,
    val contentAndRelevance: CriterionScore?,
    val quantificationAndResults: CriterionScore?,
    val technicalDepth: CriterionScore?,
    val languageAndProfessionalism: CriterionScore?
)

data class CriterionScore(
    val score: Int?,
    val justification: String?
)

data class CVEvaluation(
    val name: String?,
    val summary: String?,
    val strengths: List<String>?,
    val improvements: List<String>?,
    val scoreBreakdown: ScoreBreakdown?,
    val scorePercentage: Int
)
