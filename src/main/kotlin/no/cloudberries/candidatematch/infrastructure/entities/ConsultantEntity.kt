import com.fasterxml.jackson.databind.JsonNode
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import no.cloudberries.candidatematch.domain.candidate.Skill
import org.hibernate.annotations.Type

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
    
    @ElementCollection
    @CollectionTable(
        name = "consultant_skills",
        joinColumns = [JoinColumn(name = "consultant_id")]
    )
    @Column(name = "skill")
    val skills: MutableSet<Skill> = mutableSetOf()
)
