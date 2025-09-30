package no.cloudberries.candidatematch.infrastructure.entities

import com.fasterxml.jackson.databind.JsonNode
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Type
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "consultant")
class ConsultantEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "user_id", nullable = false)
    val userId: String,

    @Column(name = "cv_id", nullable = false)
    val cvId: String,

    @Column(name = "resume_data", columnDefinition = "json", nullable = false)
    @Type(JsonType::class)
    val resumeData: JsonNode,

    // Skills are now stored in normalized ConsultantSkillEntity table
    // Access via ConsultantSkillRepository

    @CreationTimestamp
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,

    @Version
    @Column(name = "version")
    val version: Long? = null,
)
