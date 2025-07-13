import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.Type

@Entity
@Table(name = "customer")
class CustomerEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "name")
    var name: String? = null,
    
    @Column(name = "email")
    var email: String? = null,
    
    @Column(name = "phone")
    var phone: String? = null,
    
    @Column(name = "organization")
    var organization: String? = null,

    @OneToMany(mappedBy = "customer", cascade = [CascadeType.ALL], orphanRemoval = true)
    var projectRequests: MutableSet<ProjectRequestEntity> = mutableSetOf()
)
