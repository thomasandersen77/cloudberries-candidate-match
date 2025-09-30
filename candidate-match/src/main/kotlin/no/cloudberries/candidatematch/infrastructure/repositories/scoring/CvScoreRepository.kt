package no.cloudberries.candidatematch.infrastructure.repositories.scoring

import no.cloudberries.candidatematch.infrastructure.entities.scoring.CvScoreEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CvScoreRepository : JpaRepository<CvScoreEntity, Long> {
    fun findByCandidateUserId(candidateUserId: String): CvScoreEntity?
}
