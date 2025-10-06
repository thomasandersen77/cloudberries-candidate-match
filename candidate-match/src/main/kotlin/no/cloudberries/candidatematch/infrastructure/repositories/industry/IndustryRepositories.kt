package no.cloudberries.candidatematch.infrastructure.repositories.industry

import no.cloudberries.candidatematch.infrastructure.entities.industry.CvProjectExperienceIndustryEntity
import no.cloudberries.candidatematch.infrastructure.entities.industry.IndustryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface IndustryRepository : JpaRepository<IndustryEntity, Long> {
    fun findByNameIgnoreCase(name: String): IndustryEntity?
}

@Repository
interface CvProjectExperienceIndustryRepository : JpaRepository<CvProjectExperienceIndustryEntity, Long> {
    fun findByProjectExperienceIdIn(ids: Collection<Long>): List<CvProjectExperienceIndustryEntity>

    @Modifying
    @Query("delete from CvProjectExperienceIndustryEntity c where c.projectExperienceId in :ids")
    fun deleteByProjectExperienceIdIn(ids: Collection<Long>): Int
}
