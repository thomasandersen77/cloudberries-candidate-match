package no.cloudberries.candidatematch.domain.consultant

interface ConsultantAdapter {


    fun fetchAllConsultants(): List<CvUserInfo>
    fun fetchCompleteCv(userId: String, cvId: String): Cv
    fun fetchConsultant(userId: String): Consultant
    suspend fun fetchConsultantsWithCv(): List<Consultant>

    fun exists(userId: String): Boolean
}