package no.cloudberries.ai.templates

/**
 * Prompt for classifying consultant search queries and extracting structured criteria.
 * Model must return a single JSON object (optionally wrapped in markdown fences).
 */
object QueryInterpretationPromptTemplate {

    val template: String = """
You are an expert at analyzing consultant search queries. Your job is to classify the search intent and extract structured criteria.

Analyze this user query and respond with ONLY a valid JSON object following this exact schema:

{
  "route": "structured|semantic|hybrid|rag",
  "structured": {
    "skillsAll": ["skill1", "skill2"],
    "skillsAny": ["skill3", "skill4"],
    "roles": ["role1"],
    "minQualityScore": 85,
    "locations": [],
    "availability": null,
    "publicSector": true,
    "customersAny": ["sparebank1", "bank"],
    "industries": ["finance", "public"]
  },
  "semanticText": "search description",
  "consultantName": "name if mentioned",
  "question": "specific question if asking about consultant",
  "confidence": {
    "route": 0.87,
    "extraction": 0.92
  }
}

Classification rules:
1. STRUCTURED: Clear skill requirements (e.g., "find developers who know Java and Spring")
2. SEMANTIC: Descriptive qualities (e.g., "experienced mentor who can guide juniors")
3. HYBRID: Both specific skills AND descriptive qualities
4. RAG: Asking about a specific consultant by name

Industry & customer extraction:
- Detect if query targets public sector (keywords: kommune, etat, nav, skatt, stat, offentlig, departement, direktorat)
- Detect customer/org hints (e.g., "sparebank1", "bank", "finans", "helse") and set customersAny/industries accordingly
- Keep skills normalized to lowercase

Skills normalization:
- Use lowercase, consistent naming
- "Javascript" → "javascript", "JS" → "javascript"
- "Spring Boot" → "spring", "React.js" → "react"
- "C#" → "csharp", ".NET" → "csharp"

Examples:

Query: "Find consultants who know Kotlin and Spring"
→ {"route":"structured","structured":{"skillsAll":["kotlin","spring"],"skillsAny":[],"roles":[],"minQualityScore":null},"semanticText":null,"consultantName":null,"question":null,"confidence":{"route":0.95,"extraction":0.9}}

Query: "Experienced fullstack developer who can mentor juniors"
→ {"route":"semantic","structured":null,"semanticText":"experienced fullstack developer who can mentor juniors","consultantName":null,"question":null,"confidence":{"route":0.9,"extraction":0.8}}

Query: "Senior architects with React experience and quality score above 85"
→ {"route":"hybrid","structured":{"skillsAll":[],"skillsAny":["react"],"roles":["senior","architect"],"minQualityScore":85},"semanticText":"senior architects with experience","consultantName":null,"question":null,"confidence":{"route":0.85,"extraction":0.88}}

Query: "Tell me about Thomas Andersen's experience with React"
→ {"route":"rag","structured":null,"semanticText":null,"consultantName":"Thomas Andersen","question":"experience with React","confidence":{"route":0.95,"extraction":0.9}}

Now analyze this query:
"{{user_text}}"

Respond with ONLY the JSON object, no other text:
    """.trimIndent()

    fun render(params: QueryInterpretationParams): String {
        return template.replace("{{user_text}}", params.userText)
    }
}

data class QueryInterpretationParams(
    val userText: String
)
