# INSTRUKS TIL CHATGPT: Implementering av Gemini Files API for Candidate Matching

## 🎯 Rolle
Du er en **Senior Kotlin-utvikler og Systemarkitekt** med spesialisering i:
- Spring Boot og Kotlin
- Domain-Driven Design (DDD) og Clean Architecture
- Google Cloud AI (Gemini API)
- Reactive programming (WebClient, Coroutines)

## 📋 Kontekst: Prosjektet `cloudberries-candidate-match`

### Nåværende Situasjon
Vi har et Spring Boot-basert kandidat-matching system som:
- Lagrer konsulenter, CVer, ferdigheter og oppdragsforespørsler i PostgreSQL
- Bruker Gemini AI for å analysere og rangere kandidater
- Følger Clean Architecture/DDD prinsipper
- Har allerede implementert batch-evaluering med Gemini API v1

### Utfordringen Vi Har Nå
**Problem**: Vi prøvde å bruke `gemini-3-pro-preview` modell, men fikk **400 Bad Request** fordi dette ikke er en gyldig modell-identifikator for Gemini API.

**Kortsiktig løsning**: Vi byttet til `gemini-1.5-pro` (stabil produksjonsmodell) som fungerer.

**Langsiktig mål**: Vi ønsker å implementere **Gemini Files API** med **Long Context** for å:
1. Laste opp CV-er som midlertidige filer til Gemini
2. Bruke disse fil-referansene i prompts for bedre kontekst
3. Unngå å sende hele CV-tekster inline i hver request
4. Få bedre ytelse og lavere kostnader med context caching

---

## 🏗️ Arkitektur: Hybrid Tilnærming (Anbefalt av Gemini)

Vi implementerer en **3-stegs pipeline**:

### Steg 1: Database Grovsortering (Eksisterende Logikk)
```kotlin
// I MatchesService.getTopConsultantsWithGeminiBatch()
val consultants = consultantRepository.findByRequiredSkills(requiredSkills, minMatches)
// Returnerer ~30-50 kandidater basert på SQL-matching
```

### Steg 2: Kombinert Scoring (50% Skills + 50% CV Quality)
```kotlin
val scoredConsultants = consultantScoringService.scoreConsultantsByCombinedRelevance(
    consultants = consultants,
    requiredSkills = requiredSkills,
    targetCount = 10
)
// Velger ut topp 10 kandidater
```

### Steg 3: Gemini Files API - Upload & Rank
```kotlin
// NYE KOMPONENTER SOM SKAL IMPLEMENTERES:

// 3a. Konverter CV til Markdown (on-the-fly)
val cvMarkdown = cvToMarkdownConverter.convert(consultant.cv)

// 3b. Last opp til Gemini Files API
val fileUri = geminiFilesAdapter.uploadCvFile(
    consultantId = consultant.id,
    content = cvMarkdown,
    displayName = consultant.name
)

// 3c. Kall generateContent med file_data references
val rankedCandidates = geminiFilesAdapter.rankCandidatesWithFiles(
    projectDescription = projectRequest.description,
    fileUris = listOf(fileUri1, fileUri2, ..., fileUri10),
    topN = 5
)
```

---

## 📚 Referanse-dokumentasjon: Gemini Files API

### 1. Upload Files (Resumable Upload)

**Documentation**: https://ai.google.dev/api/files

**Endpoint**: `POST https://generativelanguage.googleapis.com/upload/v1beta/files?key=API_KEY`

**Step 1 - Initiate Upload**:
```http
POST /upload/v1beta/files?key=API_KEY
Headers:
  X-Goog-Upload-Protocol: resumable
  X-Goog-Upload-Command: start
  X-Goog-Upload-Header-Content-Length: <byte_count>
  X-Goog-Upload-Header-Content-Type: text/markdown
  Content-Type: application/json

Body:
{
  "file": {
    "display_name": "CV - Thomas Andersen"
  }
}

Response Headers:
  X-Goog-Upload-URL: <upload_url>
```

**Step 2 - Upload Bytes**:
```http
POST <upload_url>
Headers:
  Content-Length: <byte_count>
  X-Goog-Upload-Offset: 0
  X-Goog-Upload-Command: upload, finalize

Body: <binary_data>

Response:
{
  "file": {
    "name": "files/abc123",
    "uri": "https://generativelanguage.googleapis.com/v1beta/files/abc123",
    "mimeType": "text/markdown",
    "sizeBytes": "12345",
    "state": "ACTIVE"
  }
}
```

### 2. Generate Content with Files (Long Context)

**Documentation**: https://ai.google.dev/gemini-api/docs/long-context

**Endpoint**: `POST https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=API_KEY`

**Request Body**:
```json
{
  "contents": [
    {
      "role": "user",
      "parts": [
        {
          "text": "Ranger disse kandidatene mot prosjektbeskrivelsen:\n\nPROSJEKT:\nVi søker en Senior Kotlin utvikler med Spring Boot erfaring...\n\nKANDIDATER:\nSe vedlagte CV-filer for detaljer."
        },
        {
          "file_data": {
            "mime_type": "text/markdown",
            "file_uri": "https://generativelanguage.googleapis.com/v1beta/files/abc123"
          }
        },
        {
          "file_data": {
            "mime_type": "text/markdown",
            "file_uri": "https://generativelanguage.googleapis.com/v1beta/files/def456"
          }
        }
      ]
    }
  ],
  "generationConfig": {
    "temperature": 0.2,
    "responseMimeType": "application/json"
  }
}
```

**Response**:
```json
{
  "candidates": [
    {
      "content": {
        "parts": [
          {
            "text": "{\"projectRequestId\":\"30\",\"ranked\":[{\"consultantId\":\"thomas.andersen\",\"score\":92,\"reasons\":[\"Extensive Kotlin experience\",\"Strong Spring Boot background\"]},{\"consultantId\":\"einar.flobak\",\"score\":88,\"reasons\":[\"Senior Java developer\",\"Good architectural skills\"]}]}"
          }
        ]
      }
    }
  ]
}
```

---

## 🛠️ Implementasjonsplan: 4 Steg

### Steg 1: Database Migration (Liquibase)

**Hva**: Legg til kolonne for å cache Gemini file URIs.

**Hvor**: Ny Liquibase changeset fil

**Kode**:
```sql
-- File: candidate-match/src/main/resources/db/changelog/changes/add-gemini-file-uri.sql

ALTER TABLE consultant_cv 
ADD COLUMN gemini_file_uri VARCHAR(512);

COMMENT ON COLUMN consultant_cv.gemini_file_uri IS 
'Cached URI from Gemini Files API upload. Format: https://generativelanguage.googleapis.com/v1beta/files/{file_id}';
```

**Registrer i master**:
```xml
<!-- File: candidate-match/src/main/resources/db/changelog/db.changelog-master.xml -->
<include file="db/changelog/changes/add-gemini-file-uri.sql"/>
```

---

### Steg 2: Domain Service - CV til Markdown Converter

**Hva**: Konverter CV-data til velformatert Markdown for Gemini.

**Hvor**: Ny service i service layer

**Kode**:
```kotlin
// File: candidate-match/src/main/kotlin/no/cloudberries/candidatematch/service/cv/CvToMarkdownConverter.kt

package no.cloudberries.candidatematch.service.cv

import no.cloudberries.candidatematch.consultant.domain.Consultant
import no.cloudberries.candidatematch.cv.domain.ConsultantCv
import org.springframework.stereotype.Service

/**
 * Converts CV data to Markdown format optimized for Gemini Files API.
 * 
 * Markdown provides better structure than plain text and is more token-efficient
 * than JSON for long-form content.
 */
@Service
class CvToMarkdownConverter {

    /**
     * Converts a consultant's CV to Markdown format.
     * 
     * @param consultant The consultant entity
     * @param cv The CV data (optional, may be embedded in consultant)
     * @return Markdown formatted CV as String
     */
    fun convert(consultant: Consultant, cv: ConsultantCv? = null): String {
        return buildString {
            appendLine("# ${consultant.name}")
            appendLine()
            
            // Basic Info
            appendLine("## Informasjon")
            appendLine("- **ID**: ${consultant.userId}")
            appendLine("- **E-post**: ${consultant.email ?: "Ikke oppgitt"}")
            appendLine("- **Tilgjengelighet**: ${consultant.availability ?: "Ikke oppgitt"}")
            appendLine()
            
            // Skills
            appendLine("## Ferdigheter")
            if (consultant.skills.isNotEmpty()) {
                consultant.skills
                    .sortedByDescending { it.experienceYears }
                    .forEach { skill ->
                        val years = skill.experienceYears?.let { " ($it år)" } ?: ""
                        appendLine("- **${skill.skillName}**$years")
                    }
            } else {
                appendLine("Ingen ferdigheter registrert.")
            }
            appendLine()
            
            // CV Quality Score (if available)
            cv?.let { cvData ->
                appendLine("## CV Kvalitet")
                appendLine("- **Score**: ${cvData.scorePercent ?: "Ikke vurdert"}/100")
                cvData.summary?.let {
                    appendLine("- **Oppsummering**: $it")
                }
                appendLine()
                
                cvData.strengths?.let {
                    appendLine("### Styrker")
                    appendLine(it)
                    appendLine()
                }
                
                cvData.potentialImprovements?.let {
                    appendLine("### Forbedringsområder")
                    appendLine(it)
                    appendLine()
                }
            }
            
            // Work Experience
            appendLine("## Erfaring")
            if (consultant.workExperience.isNotEmpty()) {
                consultant.workExperience
                    .sortedByDescending { it.startDate }
                    .forEach { exp ->
                        appendLine("### ${exp.title} @ ${exp.company}")
                        appendLine("*${exp.startDate} - ${exp.endDate ?: "Nå"}*")
                        appendLine()
                        exp.description?.let {
                            appendLine(it)
                        }
                        appendLine()
                    }
            } else {
                appendLine("Ingen arbeidserfaring registrert.")
            }
            appendLine()
            
            // Education
            appendLine("## Utdanning")
            if (consultant.education.isNotEmpty()) {
                consultant.education.forEach { edu ->
                    appendLine("### ${edu.degree} - ${edu.institution}")
                    appendLine("*${edu.year}*")
                    appendLine()
                }
            } else {
                appendLine("Ingen utdanning registrert.")
            }
            appendLine()
            
            // Certifications
            if (consultant.certifications.isNotEmpty()) {
                appendLine("## Sertifiseringer")
                consultant.certifications.forEach { cert ->
                    appendLine("- ${cert.name} (${cert.year})")
                }
                appendLine()
            }
            
            // Raw CV text (if available from Flowcase)
            consultant.cvText?.let {
                appendLine("## Fullstendig CV")
                appendLine(it)
            }
        }
    }
}
```

---

### Steg 3: Infrastructure - Gemini Files Adapter

**Hva**: WebClient-basert adapter for Gemini Files API.

**Hvor**: Infrastructure layer

**Kode**:
```kotlin
// File: candidate-match/src/main/kotlin/no/cloudberries/candidatematch/infrastructure/gemini/GeminiFilesApiAdapter.kt

package no.cloudberries.candidatematch.infrastructure.gemini

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.awaitSingle
import mu.KotlinLogging
import no.cloudberries.candidatematch.config.GeminiProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.time.Duration

/**
 * Adapter for Gemini Files API (v1beta).
 * 
 * Handles:
 * - Resumable file uploads to Gemini
 * - File URI caching
 * - Generate Content with file_data references
 * 
 * API Documentation: https://ai.google.dev/api/files
 */
@Component
class GeminiFilesApiAdapter(
    private val webClient: WebClient,
    private val geminiProperties: GeminiProperties,
    private val objectMapper: ObjectMapper
) {
    private val logger = KotlinLogging.logger {}
    
    private val apiKey = geminiProperties.apiKey
    private val uploadTimeout = Duration.ofSeconds(60)
    private val generateTimeout = Duration.ofMinutes(5)

    /**
     * Uploads CV content to Gemini Files API using resumable upload.
     * 
     * @param consultantId Unique identifier for caching
     * @param content Markdown-formatted CV content
     * @param displayName Human-readable name for file
     * @return File URI that can be used in generateContent calls
     */
    suspend fun uploadCvFile(
        consultantId: String,
        content: String,
        displayName: String
    ): String {
        logger.info { "Uploading CV file for consultant $consultantId: $displayName" }
        
        val bytes = content.toByteArray(Charsets.UTF_8)
        val numBytes = bytes.size
        
        // Step 1: Initiate resumable upload
        val uploadUrl = initiateResumableUpload(
            numBytes = numBytes,
            mimeType = "text/markdown",
            displayName = displayName
        )
        
        logger.debug { "Got upload URL: $uploadUrl" }
        
        // Step 2: Upload bytes
        val fileInfo = uploadBytes(uploadUrl, bytes, numBytes)
        
        logger.info { "Successfully uploaded CV: ${fileInfo.uri}" }
        return fileInfo.uri
    }
    
    private suspend fun initiateResumableUpload(
        numBytes: Int,
        mimeType: String,
        displayName: String
    ): String {
        val response = webClient.post()
            .uri("/upload/v1beta/files?key=$apiKey")
            .header("X-Goog-Upload-Protocol", "resumable")
            .header("X-Goog-Upload-Command", "start")
            .header("X-Goog-Upload-Header-Content-Length", numBytes.toString())
            .header("X-Goog-Upload-Header-Content-Type", mimeType)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("file" to mapOf("display_name" to displayName)))
            .retrieve()
            .toBodilessEntity()
            .timeout(uploadTimeout)
            .awaitSingle()
        
        return response.headers.getFirst("X-Goog-Upload-URL")
            ?: throw IllegalStateException("Missing X-Goog-Upload-URL header in response")
    }
    
    private suspend fun uploadBytes(
        uploadUrl: String,
        bytes: ByteArray,
        numBytes: Int
    ): GeminiFileInfo {
        return webClient.post()
            .uri(uploadUrl)
            .header("Content-Length", numBytes.toString())
            .header("X-Goog-Upload-Offset", "0")
            .header("X-Goog-Upload-Command", "upload, finalize")
            .bodyValue(bytes)
            .retrieve()
            .awaitBody<GeminiFileUploadResponse>()
            .timeout(uploadTimeout)
            .awaitSingle()
            .file
    }

    /**
     * Ranks candidates using Gemini with file references.
     * 
     * @param projectDescription Customer requirements and project details
     * @param candidateFiles List of (consultantId, fileUri) pairs
     * @param topN Number of top candidates to return
     * @return Ranked list of candidates with scores and reasons
     */
    suspend fun rankCandidatesWithFiles(
        projectRequestId: String,
        projectDescription: String,
        candidateFiles: List<CandidateFileRef>,
        topN: Int
    ): List<RankedCandidateDto> {
        logger.info { "Ranking ${candidateFiles.size} candidates with Files API for project $projectRequestId" }
        
        val systemPrompt = buildSystemPrompt()
        val userPrompt = buildUserPrompt(projectDescription, candidateFiles, topN)
        
        val requestBody = buildGenerateContentRequestWithFiles(
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            fileUris = candidateFiles.map { it.fileUri }
        )
        
        try {
            val response = webClient.post()
                .uri("/v1beta/models/${geminiProperties.model}:generateContent?key=$apiKey")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .awaitBody<Map<String, Any>>()
                .timeout(generateTimeout)
                .awaitSingle()
            
            val textResponse = extractTextFromResponse(response)
            val rankedResult = parseRankedJson(textResponse)
            
            logger.info { "Successfully ranked ${rankedResult.ranked.size} candidates" }
            return rankedResult.ranked.take(topN)
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to rank candidates with Files API for project $projectRequestId" }
            return emptyList()
        }
    }
    
    private fun buildSystemPrompt(): String = """
        Du er en teknisk rekrutteringsassistent som skal rangere konsulenter mot kundeforespørsler.
        
        Du får:
        1. En kundeforespørsel med krav og beskrivelse
        2. Flere konsulent-CVer som Markdown-filer (file_data references)
        
        Vektlegg:
        - "Må"-krav viktigere enn "bør"-krav
        - Relevant erfaring (prosjekter, teknologier, domene)
        - CV-kvalitet og fullstendighet
        - Semantisk match (ikke bare eksakte ord)
        
        Returner KUN valid JSON:
        {
          "projectRequestId": "<id>",
          "ranked": [
            {
              "consultantId": "<id>",
              "score": 0-100,
              "reasons": ["konkret grunn 1", "konkret grunn 2", "konkret grunn 3"]
            }
          ]
        }
        
        VIKTIG: Basér vurderingen på CV-innholdet i filene, ikke på generell kunnskap.
    """.trimIndent()
    
    private fun buildUserPrompt(
        projectDescription: String,
        candidateFiles: List<CandidateFileRef>,
        topN: Int
    ): String {
        val candidateList = candidateFiles.joinToString("\n") { 
            "- ${it.consultantName} (ID: ${it.consultantId})" 
        }
        
        return """
            KUNDEFORESPØRSEL:
            $projectDescription
            
            ================================================================================
            
            KONSULENTER (${candidateFiles.size} totalt):
            $candidateList
            
            (Se vedlagte CV-filer for fullstendig informasjon)
            
            ================================================================================
            
            OPPGAVE:
            Ranger de ${minOf(topN, candidateFiles.size)} best egnede konsulentene for denne kundeforespørselen.
            
            Vurder:
            1. Match mot krav (må-krav > bør-krav)
            2. Relevant erfaring fra CV
            3. Teknisk kompetanse
            4. Bransjeerfaring
            
            Returner KUN JSON som beskrevet i systemprompt.
        """.trimIndent()
    }
    
    private fun buildGenerateContentRequestWithFiles(
        systemPrompt: String,
        userPrompt: String,
        fileUris: List<String>
    ): Map<String, Any> {
        val parts = mutableListOf<Map<String, Any>>()
        
        // Add text prompt
        parts.add(mapOf("text" to "$systemPrompt\n\n$userPrompt"))
        
        // Add file references
        fileUris.forEach { uri ->
            parts.add(mapOf(
                "file_data" to mapOf(
                    "mime_type" to "text/markdown",
                    "file_uri" to uri
                )
            ))
        }
        
        return mapOf(
            "contents" to listOf(
                mapOf(
                    "role" to "user",
                    "parts" to parts
                )
            ),
            "generationConfig" to mapOf(
                "temperature" to 0.2,
                "topP" to 0.9,
                "topK" to 40,
                "maxOutputTokens" to 2048,
                "responseMimeType" to "application/json"
            )
        )
    }
    
    private fun extractTextFromResponse(response: Map<String, Any>): String {
        val candidates = response["candidates"] as? List<Map<String, Any>>
            ?: throw IllegalStateException("No candidates in response")
        
        val firstCandidate = candidates.firstOrNull()
            ?: throw IllegalStateException("Empty candidates list")
        
        val content = firstCandidate["content"] as? Map<String, Any>
            ?: throw IllegalStateException("No content in candidate")
        
        val parts = content["parts"] as? List<Map<String, Any>>
            ?: throw IllegalStateException("No parts in content")
        
        return parts.firstOrNull()?.get("text") as? String
            ?: throw IllegalStateException("No text in parts")
    }
    
    private fun parseRankedJson(json: String): RankedCandidatesResult {
        return objectMapper.readValue(json, RankedCandidatesResult::class.java)
    }
}

// Data classes
data class CandidateFileRef(
    val consultantId: String,
    val consultantName: String,
    val fileUri: String
)

data class GeminiFileUploadResponse(
    val file: GeminiFileInfo
)

data class GeminiFileInfo(
    val name: String,
    val uri: String,
    @JsonProperty("mimeType") val mimeType: String,
    @JsonProperty("sizeBytes") val sizeBytes: String,
    val state: String
)

data class RankedCandidatesResult(
    val projectRequestId: String,
    val ranked: List<RankedCandidateDto>
)

data class RankedCandidateDto(
    val consultantId: String,
    val score: Int,
    val reasons: List<String>
)
```

---

### Steg 4: Application Service - Orchestrator

**Hva**: Oppdater MatchesService til å bruke Files API.

**Hvor**: Service layer

**Kode**:
```kotlin
// File: candidate-match/src/main/kotlin/no/cloudberries/candidatematch/service/matching/MatchesService.kt

// ADD THIS NEW METHOD:

/**
 * Get top consultants using Gemini Files API (new approach).
 * 
 * Flow:
 * 1. Fetch ~30-50 candidates via SQL (existing logic)
 * 2. Score by 50% skills + 50% CV quality → Select top 10
 * 3. Upload CVs to Gemini Files API (with caching)
 * 4. Call Gemini with file references in single request
 * 5. Return ranked results
 */
private suspend fun getTopConsultantsWithGeminiFilesApi(
    projectRequest: ProjectRequest,
    topN: Int
): List<TopConsultantDto> {
    logger.info { "[MATCHING MODE] Using Gemini Files API (new approach with file uploads)" }
    
    // Step 1: Fetch candidate pool
    logger.info { "[STEP 1] Fetching candidate pool with ${projectRequest.requiredSkills.size} required skills" }
    val consultants = consultantRepository.findByRequiredSkills(
        requiredSkillNames = projectRequest.requiredSkills.map { it.name },
        minMatches = (projectRequest.requiredSkills.size * 0.3).toInt().coerceAtLeast(1),
        maxResults = 50
    )
    logger.info { "[STEP 1] Retrieved ${consultants.size} consultants from database" }
    
    // Step 2: Score and select top 10
    logger.info { "[STEP 2] Scoring consultants by 50% skills + 50% CV quality" }
    val scoredConsultants = consultantScoringService.scoreConsultantsByCombinedRelevance(
        consultants = consultants,
        requiredSkills = projectRequest.requiredSkills,
        targetCount = 10
    )
    logger.info { "[STEP 2] Selected ${scoredConsultants.size} consultants for Gemini evaluation" }
    logger.info { 
        "[STEP 2] Selected consultants: ${scoredConsultants.joinToString(", ") { it.name }}" 
    }
    
    // Step 3: Upload CVs to Gemini (with caching)
    logger.info { "[STEP 3] Uploading CVs to Gemini Files API" }
    val candidateFiles = scoredConsultants.map { consultant ->
        val fileUri = getOrUploadCvFile(consultant)
        CandidateFileRef(
            consultantId = consultant.userId,
            consultantName = consultant.name,
            fileUri = fileUri
        )
    }
    logger.info { "[STEP 3] Prepared ${candidateFiles.size} file references" }
    
    // Step 4: Call Gemini with file references
    logger.info { "[STEP 4] Calling Gemini API with ${candidateFiles.size} file references in SINGLE request" }
    val rankedCandidates = geminiFilesApiAdapter.rankCandidatesWithFiles(
        projectRequestId = projectRequest.id.toString(),
        projectDescription = buildProjectDescription(projectRequest),
        candidateFiles = candidateFiles,
        topN = topN
    )
    logger.info { "[STEP 4] Gemini returned ${rankedCandidates.size} ranked candidates" }
    
    // Step 5: Map to DTOs
    return rankedCandidates.mapNotNull { ranked ->
        val consultant = scoredConsultants.find { it.userId == ranked.consultantId }
        consultant?.let {
            TopConsultantDto(
                consultantId = it.id,
                userId = it.userId,
                name = it.name,
                email = it.email,
                matchScore = ranked.score,
                matchReasons = ranked.reasons,
                skills = it.skills.map { skill -> skill.skillName },
                cvQuality = extractCvQuality(it)
            )
        }
    }.take(topN)
}

/**
 * Gets cached file URI or uploads CV to Gemini Files API.
 * 
 * Caching strategy:
 * - Check if consultant_cv.gemini_file_uri exists and is valid
 * - If not, convert to Markdown and upload
 * - Store URI in database for future use
 */
private suspend fun getOrUploadCvFile(consultant: Consultant): String {
    // Check cache
    val cv = cvRepository.findByConsultantUserId(consultant.userId)
    if (cv?.geminiFileUri?.isNotBlank() == true) {
        logger.debug { "Using cached file URI for ${consultant.name}" }
        return cv.geminiFileUri!!
    }
    
    // Convert to Markdown
    val cvMarkdown = cvToMarkdownConverter.convert(consultant, cv)
    
    // Upload to Gemini
    val fileUri = geminiFilesApiAdapter.uploadCvFile(
        consultantId = consultant.userId,
        content = cvMarkdown,
        displayName = "CV - ${consultant.name}"
    )
    
    // Cache URI in database
    if (cv != null) {
        cv.geminiFileUri = fileUri
        cvRepository.save(cv)
    }
    
    logger.debug { "Uploaded and cached file URI for ${consultant.name}" }
    return fileUri
}

private fun buildProjectDescription(projectRequest: ProjectRequest): String {
    return buildString {
        appendLine("**Kunde**: ${projectRequest.customerName ?: "Ikke oppgitt"}")
        appendLine()
        appendLine("**Beskrivelse**:")
        appendLine(projectRequest.requestDescription)
        appendLine()
        appendLine("**Påkrevde ferdigheter**:")
        projectRequest.requiredSkills.forEach { skill ->
            appendLine("- ${skill.name}")
        }
        appendLine()
        projectRequest.startDate?.let {
            appendLine("**Startdato**: $it")
        }
        projectRequest.endDate?.let {
            appendLine("**Sluttdato**: $it")
        }
    }
}
```

---

## 🔧 Konfigurasjon

### application-local.yaml
```yaml
gemini:
  apiKey: ${GEMINI_API_KEY}
  model: gemini-1.5-pro  # Use gemini-1.5-flash for faster/cheaper processing
  flashModel: gemini-1.5-flash
  fileStoreName: not-used  # Not used with Files API approach

matching:
  provider: GEMINI
  topN: 5
  enabled: true
  model: gemini-1.5-pro  # Or gemini-1.5-flash
  timeout: 600000  # 10 minutes
  useFilesApi: true  # NEW: Toggle between inline CVs and Files API
```

### Environment Variables
```bash
export GEMINI_API_KEY="your-api-key-here"
```

---

## 🧪 Testing

### Unit Test Example
```kotlin
@Test
fun `should convert CV to markdown correctly`() {
    val consultant = createTestConsultant()
    val markdown = cvToMarkdownConverter.convert(consultant)
    
    assertThat(markdown).contains("# ${consultant.name}")
    assertThat(markdown).contains("## Ferdigheter")
    assertThat(markdown).contains("## Erfaring")
}
```

### Integration Test Example
```kotlin
@SpringBootTest
class GeminiFilesApiAdapterIT {
    
    @Autowired
    lateinit var adapter: GeminiFilesApiAdapter
    
    @Test
    fun `should upload CV and get file URI`() = runBlocking {
        val content = "# Test CV\n\nThis is a test."
        val fileUri = adapter.uploadCvFile(
            consultantId = "test-123",
            content = content,
            displayName = "Test CV"
        )
        
        assertThat(fileUri).startsWith("https://generativelanguage.googleapis.com")
    }
}
```

---

## 📊 Kostnadsanalyse

### Files API Pricing (estimat)
- **File Upload**: Gratis
- **File Storage**: Gratis (midlertidig, files expire etter 48 timer)
- **Generate Content**: Standard Gemini pricing (~$0.01 per 1K tokens)

### Sammenligning: Inline vs Files API

**Inline CVs (nåværende)**:
- 10 CVs × 2000 tokens = 20,000 tokens input
- Cost: ~$0.20 per matching

**Files API (foreslått)**:
- 10 file references × 100 tokens = 1,000 tokens input
- 10 CVs × 2000 tokens = 20,000 tokens (processed via files)
- Cost: ~$0.20 per matching (samme, men bedre context caching)

**Fordeler med Files API**:
- Bedre context caching (hvis samme CVer brukes på tvers av requests)
- Mindre request payload size
- Bedre struktur og vedlikehold

---

## 🚀 Deployment Checklist

1. ✅ Liquibase migration for `gemini_file_uri` column
2. ✅ Environment variable `GEMINI_API_KEY` satt i prod
3. ✅ Toggle `matching.useFilesApi=true` i prod config
4. ✅ Monitor logging for file upload errors
5. ✅ Test med 1-2 real project requests først
6. ✅ Gradvis rollout: 10% → 50% → 100% traffic

---

## 🎯 Output Format for Warp

Gi meg svaret strukturert slik:

1. **Filnavn** (relativ path fra `candidate-match/src/`)
2. **Kodeblokk** (fullfør hele filen, ikke bare snippets)
3. **Terminal-kommandoer** (hvis nødvendig)

Eksempel:
```kotlin
// File: main/kotlin/no/cloudberries/candidatematch/service/cv/CvToMarkdownConverter.kt

package no.cloudberries.candidatematch.service.cv

import org.springframework.stereotype.Service

@Service
class CvToMarkdownConverter {
    // ... full implementation
}
```

---

## 📝 Viktige Notater

1. **WebClient**: Bruk Spring Boot's `WebClient` for full kontroll over REST-kall
2. **Coroutines**: Bruk Kotlin coroutines (`suspend fun`) for asynkron kode
3. **Error Handling**: Graceful degradation - hvis Files API feiler, returner tom liste (ikke krasj)
4. **Caching**: Lagre `gemini_file_uri` i database for å unngå unødvendige uploads
5. **File Expiry**: Gemini files expire etter 48 timer - håndter dette med re-upload logikk
6. **MIME Type**: Bruk `text/markdown` for best results (bedre enn plain text)
7. **Model**: Bruk `gemini-1.5-pro` for kvalitet, `gemini-1.5-flash` for hastighet/kostnad

---

## 🔗 Referanser

- **Gemini Files API**: https://ai.google.dev/api/files
- **Long Context**: https://ai.google.dev/gemini-api/docs/long-context
- **Prompting Guide**: https://ai.google.dev/gemini-api/docs/prompting-strategies
- **Spring WebClient**: https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-client

---

## ✅ Completion Criteria

Implementasjonen er fullført når:

1. ✅ CVer kan lastes opp til Gemini Files API
2. ✅ File URIs caches i database (`gemini_file_uri`)
3. ✅ Matching bruker file references i stedet for inline CVs
4. ✅ Logging viser file upload og ranking steps
5. ✅ Frontend får samme respons-format som før (ingen breaking changes)
6. ✅ Error handling fungerer gracefully
7. ✅ Integration tester passerer

---

**VIKTIG**: Generer FULL kode for hver fil, ikke bare snippets. Jeg skal kunne kopiere direkte inn i Warp terminal.
