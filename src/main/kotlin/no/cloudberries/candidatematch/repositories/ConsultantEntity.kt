import com.fasterxml.jackson.databind.JsonNode
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import org.hibernate.annotations.Type

@Entity
@Table(name = "consultant")
class ConsultantEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "name", nullable = false)
    var name: String,
    
    @Column(name = "user_id", nullable = false)
    var userId: String,
    
    @Column(name = "cv_id", nullable = false)
    var cvId: String,
    
    @Column(name = "resume_data", columnDefinition = "json", nullable = false)
    @Type(JsonType::class)
    var resumeData: JsonNode,
    
    @ElementCollection
    @CollectionTable(
        name = "consultant_skills",
        joinColumns = [JoinColumn(name = "consultant_id")]
    )
    @Column(name = "skill")
    var skills: MutableSet<String> = mutableSetOf()
)
