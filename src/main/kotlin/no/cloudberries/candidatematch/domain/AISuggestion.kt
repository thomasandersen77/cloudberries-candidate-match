package no.cloudberries.candidatematch.domain

class AISuggestion(
    val id: Long? = null,
    val consultantName: String,
    val userId: String,
    val cvId: String,
    val matchScore: Double,
    val justification: String,
    val projectRequest: ProjectRequest? = null
) {

}