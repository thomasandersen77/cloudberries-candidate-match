package no.cloudberries.candidatematch.infrastructure.entities.scoring

import com.fasterxml.jackson.databind.JsonNode
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import org.hibernate.annotations.Type
import java.time.Instant

@Entity
@Table(name = "cv_score")
data class CvScoreEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(
        name = "candidate_user_id",
        nullable = false,
        unique = true
    )
    val candidateUserId: String,

    @Column(
        name = "name",
        nullable = false
    )
    val name: String,

    @Column(
        name = "score_percent",
        nullable = false
    )
    val scorePercent: Int,

    @Column(name = "summary")
    val summary: String? = null,

    @Type(JsonType::class)
    @Column(
        name = "strengths",
        columnDefinition = "jsonb"
    )
    val strengths: JsonNode? = null,

    @Type(JsonType::class)
    @Column(
        name = "potential_improvements",
        columnDefinition = "jsonb"
    )
    val potentialImprovements: JsonNode? = null,

    @Column(name = "evaluated_at")
    val evaluatedAt: Instant = Instant.now()
) {
    // JPA requires a no-arg constructor; keep it protected to avoid misuse
    protected constructor() : this(
        id = null,
        candidateUserId = "",
        name = "",
        scorePercent = 0,
        summary = null,
        strengths = null,
        potentialImprovements = null,
        evaluatedAt = Instant.EPOCH
    )
}