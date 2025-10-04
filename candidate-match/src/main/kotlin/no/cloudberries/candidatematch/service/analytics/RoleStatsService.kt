package no.cloudberries.candidatematch.service.analytics

import no.cloudberries.candidatematch.infrastructure.repositories.analytics.RoleStatsRepository
import org.springframework.stereotype.Service

@Service
class RoleStatsService(
    private val repo: RoleStatsRepository
) {
    data class RoleStat(
        val role: String,
        val consultantCount: Int,
        val percentage: Double
    )

    private val roleBuckets: Map<String, Set<String>> = mapOf(
        "System Developer" to setOf(
            "SYSTEMUTVIKLER", "SYSTEM DEVELOPER", "DEVELOPER", "SOFTWARE ENGINEER",
            "UTVIKLER", "BACKEND DEVELOPER", "FRONTEND DEVELOPER", "FULLSTACK DEVELOPER"
        ).map { it.uppercase() }.toSet(),
        "Data Engineer" to setOf(
            "DATA ENGINEER", "DATAENGINEER", "DATA-ENGINEER", "DATA PLATFORM",
            "DWH", "ETL", "DATA PLATFORM ENGINEER"
        ).map { it.uppercase() }.toSet(),
        "Project Manager" to setOf(
            "PROJECT MANAGER", "PROSJEKTLEDER", "PROJECTMANAGER", "PROSJEKT-LEDER",
            "PROSJEKT MANAGER", "PM", "SCRUM MASTER"
        ).map { it.uppercase() }.toSet(),
    )

    fun getRoleStats(): List<RoleStat> {
        val total = repo.countTotalConsultants().coerceAtLeast(1)
        val rows = repo.fetchConsultantRoles()

        val consultantByBucket = mutableMapOf<String, MutableSet<Long>>()
        fun bucketFor(roleUpper: String): String? =
            roleBuckets.entries.firstOrNull { (_, syns) -> roleUpper in syns }?.key

        rows.forEach { r ->
            val bucket = bucketFor(r.roleNameUpper) ?: return@forEach
            consultantByBucket.getOrPut(bucket) { mutableSetOf() }.add(r.consultantId)
        }

        val result = roleBuckets.keys.map { bucket ->
            val set = consultantByBucket[bucket] ?: emptySet()
            val count = set.size
            RoleStat(
                role = bucket,
                consultantCount = count,
                percentage = (count * 100.0) / total
            )
        }.sortedByDescending { it.consultantCount }
        return result
    }
}