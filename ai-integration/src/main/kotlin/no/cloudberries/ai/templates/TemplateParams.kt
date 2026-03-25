package no.cloudberries.ai.templates

data class CvReviewParams(
    val cv_json: String,
    val consultantName: String
)

fun renderCvReviewTemplate(
    template: String,
    params: CvReviewParams
): String {
    return template.replace(
        "{{cv_json}}",
        params.cv_json
    ).replace(
        "{{consultantName}}",
        params.consultantName
    )
}

data class MatchParams(
    val cv: String,
    val request: String,
    val consultantName: String
)

fun renderMatchTemplate(template: String, params: MatchParams): String {
    return template
        .replace("{{cv}}", params.cv)
        .replace("{{request}}", params.request)
        .replace("{{consultantName}}", params.consultantName)
}

data class ProjectRequestParams(
    val requestText: String,
)

fun renderProjectRequestTemplate(template: String, params: ProjectRequestParams): String {
    return template.replace("{{request_text}}", params.requestText)
}
