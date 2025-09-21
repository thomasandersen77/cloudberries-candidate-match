package no.cloudberries.candidatematch.templates

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProjectRequestPromptTemplateTest {

    @Test
    fun `renders request text into template`() {
        val text = "Dette er en forespørsel om Kotlin konsulent."
        val rendered = renderProjectRequestTemplate(
            ProjectRequestPromptTemplate.template,
            ProjectRequestParams(requestText = text)
        )
        assertTrue(rendered.contains(text))
        assertTrue(rendered.contains("project_request"))
        assertTrue(rendered.contains("must_requirements"))
        assertTrue(rendered.contains("should_requirements"))
    }
}