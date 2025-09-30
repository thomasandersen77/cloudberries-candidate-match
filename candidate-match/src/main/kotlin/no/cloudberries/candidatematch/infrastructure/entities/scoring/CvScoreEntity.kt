package no.cloudberries.candidatematch.infrastructure.entities.scoring

import com.fasterxml.jackson.databind.JsonNode
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Type

@Entity
@Table(name = "cv_score")
data class CvScoreEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "candidate_user_id", nullable = false, unique = true)
    val candidateUserId: String,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "score_percent", nullable = false)
    val scorePercent: Int,

    @Column(name = "summary", columnDefinition = "text")
    val summary: String? = null,

    @Type(JsonType::class)
    @Column(name = "strengths", columnDefinition = "json")
    val strengths: JsonNode? = null,

    @Type(JsonType::class)
    @Column(name = "potential_improvements", columnDefinition = "json")
    val potentialImprovements: JsonNode? = null,
)
