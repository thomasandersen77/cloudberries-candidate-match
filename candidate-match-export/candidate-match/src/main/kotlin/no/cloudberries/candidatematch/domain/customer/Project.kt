package no.cloudberries.candidatematch.domain.customer

class Project(
    val id: String,
    val name: String,
    val customer: Customer,
) {
    fun exists(id: String): Boolean {
        return this.id == id
    }
}
