package no.cloudberries.candidatematch.infrastructure.entities


import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import no.cloudberries.candidatematch.domain.candidate.Skill
import org.hibernate.annotations.Type

@Entity
@Table(name = "consultant")
class ConsultantEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @Column(
        name = "user_id",
        nullable = false,
        unique = true
    )
    val userId: String,

    @Column(
        name = "name",
        nullable = false
    )
    val name: String,

    @Column(
        name = "cv_id",
        nullable = false
    )
    val cvId: String,

    @Type(JsonType::class)
    @Column(
        name = "resume_data",
        columnDefinition = "jsonb",
        nullable = false
    )
    val resumeData: JsonNode,

    @ElementCollection(targetClass = Skill::class)
    @CollectionTable(
        name = "consultant_skills",
        joinColumns = [JoinColumn(name = "consultant_id")] // must be BIGINT in DB
    )
    @Enumerated(EnumType.STRING)
    @Column(
        name = "skill",
        nullable = false
    )
    val skills: Set<Skill> = emptySet()
) {
    // Required by JPA/Hibernate. Use at least protected visibility.
    protected constructor() : this(
        id = null,
        name = "",
        userId = "",
        cvId = "",
        resumeData = JsonNodeFactory.instance.objectNode(),
        skills = mutableSetOf()
    )
}

