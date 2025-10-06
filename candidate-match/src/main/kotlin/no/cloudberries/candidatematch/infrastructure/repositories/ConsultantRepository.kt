package no.cloudberries.candidatematch.infrastructure.repositories

import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.data.repository.query.Param
import no.cloudberries.candidatematch.domain.candidate.Skill
import no.cloudberries.candidatematch.service.skills.ConsultantSkillReader
import no.cloudberries.candidatematch.service.skills.SkillAggregateRow

@Repository
interface ConsultantRepository : JpaRepository<ConsultantEntity, Long>, ConsultantSkillReader {
    fun findByUserId(userId: String): ConsultantEntity?
    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<ConsultantEntity>

    // Flat projections to avoid instantiating entities (no-arg constructor not required)
    @Query("select c.id as id, c.userId as userId, c.name as name, c.cvId as cvId from ConsultantEntity c")
    fun findAllFlat(): List<ConsultantFlatView>

    @Query(
        value = "select c.id as id, c.userId as userId, c.name as name, c.cvId as cvId from ConsultantEntity c",
        countQuery = "select count(c) from ConsultantEntity c"
    )
    fun findAllFlat(pageable: Pageable): Page<ConsultantFlatView>

    // Deprecated: Skills are now in the normalized ConsultantSkillEntity table
    @Deprecated("Use SkillService.getConsultantSkills instead")
    fun findSkillsByConsultantIds(@Param("ids") ids: Collection<Long>): List<ConsultantSkillRow> = emptyList()

    // --- ConsultantSkillReader implementation ---
    // Note: These methods now use the SkillService for aggregation
    @Query(
        "SELECT new no.cloudberries.candidatematch.service.skills.SkillAggregateRow(" +
            "sk.name, c.userId, c.name, c.cvId) " +
            "FROM ConsultantEntity c " +
            "JOIN ConsultantSkillEntity cs ON c.id = cs.consultantId " +
            "JOIN SkillEntity sk ON cs.skillId = sk.id"
    )
    override fun findAllSkillAggregates(): List<SkillAggregateRow>

    @Query(
        "SELECT new no.cloudberries.candidatematch.service.skills.SkillAggregateRow(" +
            "sk.name, c.userId, c.name, c.cvId) " +
            "FROM ConsultantEntity c " +
            "JOIN ConsultantSkillEntity cs ON c.id = cs.consultantId " +
            "JOIN SkillEntity sk ON cs.skillId = sk.id " +
            "WHERE (:skillNames is null or sk.name in :skillNames)"
    )
    override fun findSkillAggregates(
        @Param("skillNames") skillFilters: Collection<String>?
    ): List<SkillAggregateRow>

    fun existsByUserId(userId: String): Boolean
}

// Projection types
interface ConsultantFlatView {
    fun getId(): Long
    fun getUserId(): String
    fun getName(): String
    fun getCvId(): String
}
interface ConsultantSkillRow {
    fun getConsultantId(): Long
    fun getSkill(): Skill?
}
