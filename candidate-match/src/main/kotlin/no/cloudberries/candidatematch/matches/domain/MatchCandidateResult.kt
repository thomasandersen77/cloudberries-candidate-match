package no.cloudberries.candidatematch.matches.domain

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * Domain entity representing an individual candidate result within a matching computation.
 * 
 * This entity follows DDD principles by encapsulating validation logic and maintaining
 * referential integrity with the parent ProjectMatchResult.
 */
@Entity
@Table(name = "match_candidate_result")
class MatchCandidateResult(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_result_id", nullable = false)
    var matchResult: ProjectMatchResult? = null,

    @Column(name = "consultant_id", nullable = false)
    val consultantId: Long,

    @Column(name = "match_score", nullable = false, precision = 5, scale = 4)
    val matchScore: BigDecimal,

    @Column(name = "match_explanation", columnDefinition = "text")
    val matchExplanation: String?,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
) {
    
    companion object {
        val MIN_SCORE = BigDecimal.ZERO
        val MAX_SCORE = BigDecimal.ONE
        const val ACCEPTABLE_SCORE_THRESHOLD = 0.6
    }
    
    /**
     * Validates the candidate result according to business rules.
     * Score must be between 0 and 1, and consultant ID must be positive.
     */
    fun isValid(): Boolean {
        return consultantId > 0 && 
               matchScore >= MIN_SCORE && 
               matchScore <= MAX_SCORE
    }
    
    /**
     * Checks if the match score meets the minimum acceptable threshold.
     */
    fun hasAcceptableScore(): Boolean {
        return matchScore.compareTo(BigDecimal.valueOf(ACCEPTABLE_SCORE_THRESHOLD)) >= 0
    }
    
    /**
     * Gets a human-readable score percentage.
     */
    fun getScorePercentage(): Int {
        return (matchScore * BigDecimal.valueOf(100)).toInt()
    }
    
    /**
     * Checks if this candidate result has an explanation.
     */
    fun hasExplanation(): Boolean = !matchExplanation.isNullOrBlank()
    
    /**
     * Gets a safe explanation or default message.
     */
    fun getExplanationOrDefault(): String {
        return matchExplanation?.takeIf { it.isNotBlank() } 
            ?: "No explanation provided for this match"
    }
    
    /**
     * Compares this candidate result with another based on match score.
     */
    fun hasHigherScoreThan(other: MatchCandidateResult): Boolean {
        return matchScore > other.matchScore
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MatchCandidateResult
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: javaClass.hashCode()
    }

    override fun toString(): String {
        return "MatchCandidateResult(id=$id, consultantId=$consultantId, " +
               "matchScore=$matchScore, hasExplanation=${hasExplanation()}, createdAt=$createdAt)"
    }
}