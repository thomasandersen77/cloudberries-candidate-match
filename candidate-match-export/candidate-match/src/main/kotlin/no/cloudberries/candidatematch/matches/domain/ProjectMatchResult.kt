package no.cloudberries.candidatematch.matches.domain

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.OffsetDateTime

/**
 * Domain entity representing the result of a matching computation for a project request.
 * 
 * This entity follows the DDD principle of keeping domain logic within the entity.
 * It maintains a one-to-many relationship with MatchCandidateResult entities.
 */
@Entity
@Table(name = "project_match_result")
class ProjectMatchResult(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "project_request_id", nullable = false)
    val projectRequestId: Long,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),

    @OneToMany(
        mappedBy = "matchResult", 
        cascade = [CascadeType.ALL], 
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    val candidateResults: MutableList<MatchCandidateResult> = mutableListOf()
) {
    
    /**
     * Adds a candidate result to this match result.
     * Follows DDD principle by encapsulating the bidirectional relationship.
     */
    fun addCandidateResult(candidateResult: MatchCandidateResult) {
        candidateResults.add(candidateResult)
        candidateResult.matchResult = this
    }
    
    /**
     * Gets the top N candidate results ordered by match score.
     * Domain logic kept within the entity.
     */
    fun getTopCandidates(limit: Int = 10): List<MatchCandidateResult> {
        return candidateResults
            .sortedByDescending { it.matchScore }
            .take(limit)
    }
    
    /**
     * Checks if this match result has any candidates.
     */
    fun hasCandidates(): Boolean = candidateResults.isNotEmpty()
    
    /**
     * Gets the total number of matched candidates.
     */
    fun getTotalMatches(): Int = candidateResults.size
    
    /**
     * Validates the match result according to business rules.
     */
    fun isValid(): Boolean {
        return projectRequestId > 0 && candidateResults.all { it.isValid() }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ProjectMatchResult
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: javaClass.hashCode()
    }

    override fun toString(): String {
        return "ProjectMatchResult(id=$id, projectRequestId=$projectRequestId, " +
               "candidateCount=${candidateResults.size}, createdAt=$createdAt)"
    }
}