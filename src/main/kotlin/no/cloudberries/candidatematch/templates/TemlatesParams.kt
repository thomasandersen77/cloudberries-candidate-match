package no.cloudberries.candidatematch.templates

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