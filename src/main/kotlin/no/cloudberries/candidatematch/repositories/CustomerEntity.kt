import jakarta.persistence.*
import no.cloudberries.candidatematch.repositories.ProjectRequestEntity

@Entity
@Table(name = "customer")
class CustomerEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "name")
    val name: String? = null,
    
    @Column(name = "email")
    val email: String? = null,
    
    @Column(name = "phone")
    val phone: String? = null,
    
    @Column(name = "organization")
    val organization: String? = null,

    @OneToMany(mappedBy = "customer", cascade = [CascadeType.ALL], orphanRemoval = true)
    val projectRequestEntities: MutableSet<ProjectRequestEntity> = mutableSetOf()
)
