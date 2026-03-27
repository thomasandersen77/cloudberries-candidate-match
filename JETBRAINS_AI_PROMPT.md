# JetBrains AI Diagnostic Prompt

## Problem Statement

The Cloudberries Candidate Match application is experiencing a 403 Forbidden error when the frontend tries to access backend API endpoints. This is a critical issue preventing the application from functioning.

## System Overview

### Backend
- **Location**: `/Users/tandersen/git/cloudberries-candidate-match`
- **Tech Stack**: Kotlin, Spring Boot 3.3.4, PostgreSQL with pgvector
- **Deployed to**: Azure Container App (cloudberries-candidate-match-ca)
- **URL**: https://cloudberries-candidate-match-ca.whitesand-767916af.westeurope.azurecontainerapps.io

### Frontend
- **Location**: `/Users/tandersen/git/cloudberries-candidate-match-web`
- **Tech Stack**: React, TypeScript, Vite
- **API Client**: Generated from OpenAPI spec via `npm run gen:api`

### Additional Integration (In Progress)
- Ollama running as a container service for CV expert model
- Backend integrating with local Ollama for AI-powered candidate matching

## Current Issue Details

### Symptoms
1. **Frontend error**: Browser console shows `GET https://cloudberries-candidate-match-ca.whitesand-767916af.westeurope.azurecontainerapps.io/consultants/with-cv?onlyActiveCv=true 403 (Forbidden)`
2. **Multiple endpoints affected**:
   - `/consultants/with-cv`
   - `/cv-score/all`
   - `/chatbot/analyze`
3. **Working endpoints**:
   - `/actuator/health` returns 200 OK
   - `/swagger-ui/index.html` returns 200 OK
   - `/v3/api-docs` returns 200 OK

### What We've Verified

#### Infrastructure Level (ALL CLEAR)
- ✅ Azure Container App authentication is **disabled** (no auth config exists)
- ✅ No IP restrictions on ingress
- ✅ Container App is running (2 replicas, healthy)
- ✅ Latest container image deployed: `ghcr.io/thomasandersen77/cloudberries-candidate-match:32b87d351e9c7f022715ab872fa52becd51a7d85`

#### Backend Code Level (ALL CLEAR)
- ✅ CORS configured to allow all origins: `allowedOriginPatterns("*")` in `WebCorsConfig.kt`
- ✅ No Spring Security dependency in `pom.xml`
- ✅ No `@PreAuthorize` or security annotations found on controllers
- ✅ Endpoint exists and is documented in OpenAPI spec with no security requirements
- ✅ Controller code is simple and has no authorization logic

#### Frontend Code Level (UNKNOWN)
- ❓ API client configuration
- ❓ Request headers being sent
- ❓ CORS preflight handling

## Questions for JetBrains AI

### Backend Analysis (`/Users/tandersen/git/cloudberries-candidate-match`)

1. **Identify the 403 source**: Search the entire backend codebase for any code that could return HTTP 403:
   - Custom filters or interceptors that might reject requests
   - Exception handlers that convert exceptions to 403
   - Any validation logic that fails silently
   - Check `candidate-match/src/main/kotlin/` for any custom security or validation

2. **Verify endpoint accessibility**: Examine these files:
   - `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/controllers/consultants/ConsultantCvQueryController.kt`
   - `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/service/consultants/ConsultantWithCvService.kt`
   - `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/config/WebCorsConfig.kt`
   
   Verify there's nothing that would cause a 403 for anonymous requests.

3. **Check for hidden configuration**: Search for:
   - Any YAML/properties files that might enable authentication
   - Rate limiting configuration (bucket4j is a dependency)
   - Custom error handling that might mask the real issue
   - Environment-specific configuration in `candidate-match/src/main/resources/`

4. **Examine application startup**: Check `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/Main.kt` for any custom configuration that might affect request handling.

### Frontend Analysis (`/Users/tandersen/git/cloudberries-candidate-match-web`)

1. **API client configuration**: Examine how the OpenAPI-generated client is configured:
   - Base URL configuration
   - Default headers being sent
   - Request/response interceptors
   - CORS mode settings

2. **Request inspection**: Find where these API calls originate:
   - `/consultants/with-cv?onlyActiveCv=true`
   - `/cv-score/all`
   - `/chatbot/analyze`
   
   Check what headers are being sent with these requests.

3. **CORS preflight**: Verify if the frontend is:
   - Sending OPTIONS requests before GET/POST
   - Including custom headers that trigger preflight
   - Properly handling CORS errors

4. **Environment configuration**: Check for:
   - API base URL configuration
   - Any authentication tokens or API keys being sent
   - Environment-specific settings that might affect production

## Expected Output from JetBrains AI

### For Backend
1. **Exact location** of code causing 403 response
2. **Recommended fix** with specific file paths and code changes
3. **Why** `/actuator/health` works but `/consultants/with-cv` doesn't

### For Frontend
1. **Request headers** being sent to failing endpoints
2. **Any client-side configuration** that might cause issues
3. **Recommended changes** to fix the integration

## Additional Context

### Backend Architecture
- Modular monolith with modules:
  - `candidate-match` (main executable)
  - `ai-integration` (AI/RAG services)
  - `ai-platform-contracts` (shared interfaces)
  - `teknologi-barometer-service` (separate bounded context)

### Known Working Features
- Health check endpoint
- Swagger UI
- OpenAPI documentation
- Database connectivity (PostgreSQL with pgvector)

### Recent Changes
- Updated CORS to allow all origins
- Attempted to disable Azure Container App authentication
- Rebuilt and redeployed container multiple times

### Deployment Info
- Spring Boot app runs on port 8080
- Container exposes port 8080 externally
- No reverse proxy or API gateway in front of the app
- Azure Container App provides TLS termination

## Urgency
This is a **critical production issue** - the frontend cannot communicate with the backend at all except for health checks and documentation endpoints.

---

## How to Use This Prompt

### In Backend Project (IntelliJ IDEA)
1. Open `/Users/tandersen/git/cloudberries-candidate-match` in IntelliJ IDEA
2. Open JetBrains AI Assistant
3. Paste this entire document
4. Ask: "Please analyze the backend codebase based on this diagnostic report and identify why API endpoints return 403 while health checks work"

### In Frontend Project (WebStorm/VS Code)
1. Open `/Users/tandersen/git/cloudberries-candidate-match-web`
2. Open JetBrains AI Assistant (or GitHub Copilot)
3. Paste this entire document
4. Ask: "Please analyze the frontend codebase and identify what might be causing 403 errors when calling the backend API"

### Follow-up Questions
Once AI identifies potential issues, ask:
- "Show me the exact code changes needed to fix this"
- "Are there any other endpoints that might have the same issue?"
- "How can we prevent this from happening again?"
