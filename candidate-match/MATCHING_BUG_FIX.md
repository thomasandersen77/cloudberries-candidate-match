# Matching Bug Fix - Verbose Requirements Issue

## Problem Summary

When uploading a customer project request PDF (e.g., from Politiet/Police), **zero consultants were being matched** despite having many qualified consultants in the database.

## Root Cause

The issue was in `ConsultantWithCvService.getTopConsultantsBySkills()` at line 58-89.

### How Matching Was Supposed to Work

1. **AI extracts requirements** from the PDF → 16 requirements like:
   - "Utdannelse på minimum bachelor-nivå innen IT- eller teknologiledelse"
   - "Minst 12 mnd erfaring siste 5 år som fullstack-utvikler (Java/Kotlin)"
   - "Dokumentert erfaring med React og TypeScript"
   - "Dokumentert erfaring med SQL og utvikling mot databaser"

2. **System filters consultants** by comparing these requirements against consultant skills

3. **AI ranks the filtered consultants** to find best matches

### What Was Actually Happening

**The skill matching logic was doing exact string matching** between:
- **Requirement strings** (long, verbose descriptions in Norwegian)
- **Consultant skill tags** (short technical keywords like "Java", "React", "SQL")

**Example of failed match:**
```
Requirement: "Minst 12 mnd erfaring siste 5 år som fullstack-utvikler (Java/Kotlin)"
Consultant skills: ["Java", "Kotlin", "Spring Boot"]

String comparison: "MINST 12 MND ERFARING..." == "JAVA" ? ❌ NO MATCH
                   "MINST 12 MND ERFARING..." == "KOTLIN" ? ❌ NO MATCH
                   "MINST 12 MND ERFARING..." == "SPRING BOOT" ? ❌ NO MATCH
```

**Result:** Zero skill overlap → consultant filtered out → **no matches found**

This happened for **all 120 consultants**, leaving zero candidates for AI ranking.

### Log Evidence

```
2025-11-23 14:27:34,011 INFO   [] n.c.c.m.s.ProjectMatchingServiceImpl.getConsultantsForMatching:295: 
  Selecting consultants for project 28. Required skills: 
  Utdannelse på minimum bachelor-nivå innen IT- eller teknologiledelse, eller master innen informatikk/matematikk/statistikk, 
  Minst 12 mnd erfaring siste 5 år som fullstack-utvikler (Java/Kotlin), ...

2025-11-23 14:27:35,729 WARN   [] n.c.c.m.s.ProjectMatchingServiceImpl.getConsultantsForMatching:308: 
  No consultants available for matching

2025-11-23 14:27:33,952 INFO   [6f0b9823] n.c.c.s.ProjectRequestService.findMatchingConsultants:185: 
  Generated 0 valid suggestions from 0 consultants
```

## The Fix

### Solution: Extract Technology Keywords from Verbose Requirements

Modified `ConsultantWithCvService.kt` to:

1. **Extract technical keywords** from verbose requirement descriptions
2. **Use partial/fuzzy matching** instead of exact string matching
3. **Fall back to all consultants** if no keywords can be extracted

### New Method: `extractSkillKeywords()`

```kotlin
private fun extractSkillKeywords(requirements: List<String>): List<String> {
    val keywords = mutableSetOf<String>()
    
    // Define common technology keywords
    val techKeywords = setOf(
        "Java", "Kotlin", "React", "TypeScript", "JavaScript", "CSS", "HTML",
        "SQL", "PostgreSQL", "MySQL", "MongoDB",
        "Docker", "Kubernetes", "REST", "JSON", "XML",
        "Spring", "Spring Boot", "Microservices", "Material UI",
        "Git", "CI/CD", "DevOps", "Agile", "Scrum",
        "Domain Driven Design", "DDD", "Test Driven Development", "TDD",
        // ... more keywords
    )
    
    requirements.forEach { requirement ->
        val upperReq = requirement.uppercase()
        
        // Check for each tech keyword
        techKeywords.forEach { tech ->
            if (upperReq.contains(tech.uppercase())) {
                keywords.add(tech)
            }
        }
        
        // Extract slash-separated skills: "Java/Kotlin" -> ["Java", "Kotlin"]
        val slashPattern = """([A-Z][a-z]+(?:[A-Z][a-z]+)*)/([A-Z][a-z]+(?:[A-Z][a-z]+)*)""".toRegex()
        slashPattern.findAll(requirement).forEach { match ->
            keywords.add(match.groupValues[1])
            keywords.add(match.groupValues[2])
        }
    }
    
    return keywords.toList()
}
```

### Example: Before vs After

**Input Requirement:**
```
"Minst 12 mnd erfaring siste 5 år som fullstack-utvikler (Java/Kotlin)"
```

**Before Fix:**
- Compared verbatim: `"MINST 12 MND ERFARING SISTE 5 ÅR SOM FULLSTACK-UTVIKLER (JAVA/KOTLIN)"` 
- Against: `["JAVA", "KOTLIN", "SPRING BOOT"]`
- Result: **NO MATCHES** (0 overlap)

**After Fix:**
- Extracts keywords: `["Java", "Kotlin"]`
- Compares: `["JAVA", "KOTLIN"]` vs `["JAVA", "KOTLIN", "SPRING BOOT"]`
- Result: **2 MATCHES** (100% overlap!)

### Updated Scoring Logic

```kotlin
// Use partial matching: consultant skill contains or is contained in required skill
var matchCount = 0
for (consultantSkill in consultantSkillSet) {
    for (requiredSkill in requiredSkillSet) {
        if (consultantSkill.contains(requiredSkill) || requiredSkill.contains(consultantSkill)) {
            matchCount++
            break // Count each consultant skill once
        }
    }
}

// Calculate score based on match count and CV quality
val matchRatio = if (requiredSkillSet.isNotEmpty()) matchCount.toDouble() / requiredSkillSet.size else 0.0
val cvQualityBonus = if (consultant.cvs.isNotEmpty()) 0.1 else 0.0

val totalScore = matchRatio + cvQualityBonus
```

### Removed Hard Filter

**Before:**
```kotlin
return scoredConsultants
    .filter { it.second > 0.0 } // ❌ This removed ALL consultants when no skills matched
    .sortedByDescending { it.second }
    .take(limit)
    .map { it.first }
```

**After:**
```kotlin
return scoredConsultants
    .sortedByDescending { it.second }  // ✅ Now returns top consultants even with 0.0 score
    .take(limit)
    .map { it.first }
```

## Expected Behavior After Fix

1. **PDF uploaded** → Gemini extracts verbose requirements
2. **Keywords extracted** → ["Java", "Kotlin", "React", "TypeScript", "SQL", "Docker", "Kubernetes", "Material UI", "REST", "JSON"]
3. **Consultants filtered** → Top 50 consultants with matching skills
4. **Consultants scored** → Combined skill match (70%) + CV quality (30%)
5. **Top 5-15 selected** → Best candidates sent to AI for detailed ranking
6. **AI analyzes** → Generates justifications and final matches
7. **Results returned** → User sees matched consultants with scores

### For the Police Request Specifically

Given the requirements mention:
- Java/Kotlin ✅
- React & TypeScript ✅
- SQL ✅
- Docker & Kubernetes ✅
- Material UI ✅
- REST & JSON ✅
- Domain Driven Design ✅
- Test Driven Development ✅

**Expected matches:** You (Thomas Andersen) and many other consultants who have these skills should now appear in the results.

## Testing

To verify the fix works:

1. **Restart the application:**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

2. **Re-upload the Police PDF** at `http://localhost:8080/swagger-ui/index.html`

3. **Check logs** - you should see:
   ```
   INFO  Selecting consultants for project X. Required skills: ...
   INFO  Retrieved 50 consultants from initial pool
   INFO  Selected 10 consultants for AI matching: Thomas Andersen, ...
   ```

4. **Verify results** - Navigate to the project request and see matched consultants

## Files Changed

- `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/service/consultants/ConsultantWithCvService.kt`
  - Modified `getTopConsultantsBySkills()` (lines 58-150)
  - Added `extractSkillKeywords()` helper method
  - Improved scoring logic with partial matching
  - Removed hard filter that excluded all non-matching consultants
