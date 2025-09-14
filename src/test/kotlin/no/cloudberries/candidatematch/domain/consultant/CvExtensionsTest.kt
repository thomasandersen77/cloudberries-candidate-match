package no.cloudberries.candidatematch.domain.consultant

import no.cloudberries.candidatematch.domain.candidate.Skill
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Year
import java.time.YearMonth

class CvExtensionsTest {

    @Test
    fun `toFlatText flattens key sections of CV`() {
        val cv = Cv(
            id = "cv1",
            keyQualifications = listOf(
                KeyQualification(
                    "Kotlin",
                    "Experienced Kotlin dev"
                )
            ),
            workExperiences = listOf(
                WorkExperience(
                    "Acme",
                    TimePeriod(
                        YearMonth.of(
                            2020,
                            1
                        ),
                        YearMonth.of(
                            2022,
                            12
                        )
                    )
                )
            ),
            projectExperiences = listOf(
                ProjectExperience(
                    customer = "BigCorp",
                    description = "Backend services",
                    longDescription = "Built microservices",
                    period = TimePeriod(
                        YearMonth.of(
                            2021,
                            1
                        ),
                        YearMonth.of(
                            2021,
                            12
                        )
                    ),
                    roles = listOf(
                        Role(
                            "Developer",
                            "Implemented APIs"
                        )
                    ),
                    skillsUsed = listOf(
                        Skill(
                            "KOTLIN",
                            3
                        )
                    )
                )
            ),
            educations = listOf(
                Education(
                    "MSc CS",
                    "NTNU",
                    TimePeriod(
                        YearMonth.of(
                            2012,
                            9
                        ),
                        YearMonth.of(
                            2017,
                            6
                        )
                    )
                )
            ),
            certifications = listOf(
                Certification(
                    "GCP SA",
                    Year.of(2023)
                )
            ),
            courses = listOf(
                Course(
                    "Kubernetes",
                    "Coursera",
                    Year.of(2022)
                )
            ),
            languages = listOf(
                LanguageSkill(
                    "Norwegian",
                    "Native"
                )
            ),
            skillCategories = listOf(
                no.cloudberries.candidatematch.domain.consultant.SkillCategory(
                    "Backend",
                    listOf(
                        no.cloudberries.candidatematch.domain.consultant.Skill(
                            "KOTLIN",
                            5
                        )
                    )
                )
            )
        )

        val text = cv.toFlatText()
        // Basic sanity checks
        listOf(
            "Experienced Kotlin dev",
            "Acme",
            "BigCorp",
            "Developer",
            "KOTLIN",
            "MSc CS",
            "GCP SA",
            "Kubernetes",
            "Norwegian"
        ).forEach {
            assert(text.contains(it)) { "Expected flattened text to contain '$it'" }
        }
    }

    @Test
    fun `skillMatchScore returns ratio percentage of required skills`() {
        val consultant = Consultant.builder(
            id = "u1",
            defaultCvId = "cv1"
        )
            .withPersonalInfo(
                PersonalInfo(
                    "Jane",
                    "jane@example.com",
                    Year.of(1990)
                )
            )
            .withCv(Cv("cv1"))
            .withCvAsJson("{}")
            .withSkills(
                listOf(
                    no.cloudberries.candidatematch.domain.consultant.Skill(
                        "KOTLIN",
                        5
                    ),
                    no.cloudberries.candidatematch.domain.consultant.Skill(
                        "JAVA",
                        10
                    )
                )
            )
            .build()

        val required = setOf(
            Skill.KOTLIN,
            Skill.JAVA,
            Skill.REACT
        )
        val score = consultant.skillMatchScore(required)
        assertEquals(
            66,
            score
        ) // 2/3 -> 66%
    }
}
