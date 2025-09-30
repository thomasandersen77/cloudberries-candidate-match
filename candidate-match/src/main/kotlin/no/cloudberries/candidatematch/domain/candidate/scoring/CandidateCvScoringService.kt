package no.cloudberries.candidatematch.domain.candidate.scoring

import no.cloudberries.candidatematch.domain.candidate.Candidate
import no.cloudberries.candidatematch.domain.consultant.ConsultantAdapter
import org.springframework.stereotype.Component

@Component
class CandidateCvScoringService(
    val consultantAdapter: ConsultantAdapter
) {


    fun getAllCandidates(): List<Candidate> {
        val consultants = consultantAdapter.fetchAllConsultants()
        return consultants.map {
            Candidate(
                id = it.userId,
                name = it.name,
                birthYear = it.bornYear
            )
        }
    }

}