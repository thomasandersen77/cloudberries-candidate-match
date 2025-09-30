package no.cloudberries.barometer.domain

import com.fasterxml.jackson.annotation.JsonValue

enum class DemandPriority(@JsonValue val value: String) {
    CRITICAL("critical"),
    HIGH("high"), 
    MEDIUM("medium"),
    NICE_TO_HAVE("nice_to_have")
}

enum class SeniorityLevel(@JsonValue val value: String) {
    JUNIOR("junior"),
    MID("mid"),
    SENIOR("senior"), 
    PRINCIPAL("principal"),
    EXPERT("expert")
}

enum class RoleType(@JsonValue val value: String) {
    DEVELOPER("developer"),
    ARCHITECT("architect"),
    LEAD("lead"),
    MANAGER("manager"),
    CONSULTANT("consultant"),
    SPECIALIST("specialist")
}

enum class LocationType(@JsonValue val value: String) {
    REMOTE("remote"),
    HYBRID("hybrid"),
    ON_SITE("on_site")
}

enum class CompetencyArea(@JsonValue val value: String) {
    TECHNICAL("technical"),
    DOMAIN("domain"),
    METHODOLOGICAL("methodological"),
    LEADERSHIP("leadership")
}

enum class TrendDirection(@JsonValue val value: String) {
    UP("up"),
    DOWN("down"),
    STABLE("stable")
}

enum class ExtractionStatus(@JsonValue val value: String) {
    EXTRACTED("extracted"),
    FAILED("failed"),
    PENDING("pending")
}

// Technology item extracted from job descriptions
data class TechItem(
    val name: String,
    val category: String,
    val priority: DemandPriority,
    val context: String?
)

// Competency item extracted from job descriptions  
data class CompetencyItem(
    val name: String,
    val area: CompetencyArea,
    val priority: DemandPriority,
    val experienceRequired: String?
)