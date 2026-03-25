# Consultant Matching Feature Implementation

## Overview

This document outlines the implementation of an automated consultant matching system that evaluates consultants against project requests. The matching functionality operates in two scenarios:

1. **Automatic Background Matching**: Triggered automatically when a project request is uploaded
2. **Manual Matching**: Triggered from the frontend `/matches` page via a controller endpoint

## Key Requirements

- **No Terminal Interaction**: The implementation executes without stopping for user confirmation
- **Background Processing**: Matching runs automatically on project request upload
- **Manual Trigger**: Button on `/matches` page to trigger matching via controller
- **Controller → Service → Repository Pattern**: Follow established architectural patterns
- **Reuse Existing Components**: Leverage existing code and infrastructure where possible
- **Non-Interactive**: Plan executes fully automated but allows testing during process

## Prerequisites

- Projects located locally:
  - Backend: `/Users/tandersen/git/cloudberries-candidate-match/candidate-match`
  - Frontend: `/Users/tandersen/git/cloudberries-candidate-match-web`
- Database running on `localhost:5433`
- Gemini model: `gemini-2.5-pro`
- Terminal has access to `mvn`, `npm`, `liquibase` and `jq`
- Java SDK version 21.0.7-tem (to avoid bytebuddy conflicts)

## Implementation Architecture

```
Project Request Upload → Event → Background Matching Service
Frontend /matches Button → Controller → Service → Repository
                                ↓
                        AI Matching Engine
                                ↓
                          Match Results DB
```

## Implementation Plan

### Phase 1: Read Project Context

```warp-runnable-command
cd /Users/tandersen/git/cloudberries-candidate-match/candidate-match
bat ../WARP.md || true
bat ../README.md || true
```

```warp-runnable-command
cd /Users/tandersen/git/cloudberries-candidate-match-web
bat WARP.md || true
bat README.md || true
```

### Phase 2: Backend Implementation - Kotlin Components

#### Create Directory Structure

```warp-runnable-command
cd /Users/tandersen/git/cloudberries-candidate-match/candidate-match/src/main/kotlin/no/cloudberries/candidatematch
mkdir -p matches/{dto,domain,repository,service,controller,event}
```

#### Data Transfer Objects (DTOs)

Create Kotlin data classes for:
- `ProjectRequestSummaryDto`: Project request summary information
- `MatchCandidateDto`: Candidate match results with scores and explanation
- `MatchTop10Response`: Response wrapper for top 10 matches
- `TriggerMatchingRequest`: Request for manual matching trigger
- `TriggerMatchingResponse`: Response for trigger operation

#### Domain Entities

Create JPA entities in Kotlin:
- `MatchResult`: Main match result entity with project reference and timestamp
- `MatchCandidateScore`: Individual candidate score entity with match details
- Proper JPA relationships and constraints

#### Repository Layer

Create Spring Data JPA repositories:
- `MatchResultRepository`: Query match results by project request
- `MatchCandidateScoreRepository`: Query candidate scores with sorting
- Custom query methods for top matches and filtering

#### Service Layer with Background Processing

Create service interfaces and implementations:
- `ProjectMatchingService`: Interface defining matching operations
- `ProjectMatchingServiceImpl`: Implementation with async processing
- Methods for:
  - `computeMatches()`: Core matching logic
  - `getMatchesForProject()`: Retrieve existing matches
  - `triggerAsyncMatching()`: Manual trigger for matching
  - `onProjectRequestUploaded()`: Auto-trigger on upload

#### Database Schema Migration

```warp-runnable-command
cat > /Users/tandersen/git/cloudberries-candidate-match/candidate-match/src/main/resources/db/changelog/2025-10-07-matches.yaml <<'EOF'
databaseChangeLog:
  - changeSet:
      id: matches-20251007
      author: warp
      changes:
        - createTable:
            tableName: project_match_result
            columns:
              - column: { name: id, type: BIGSERIAL, constraints: { primaryKey: true } }
              - column: { name: project_request_id, type: BIGINT, constraints: { nullable: false } }
              - column: { name: created_at, type: timestamptz, defaultValueComputed: CURRENT_TIMESTAMP }
              - column: { name: updated_at, type: timestamptz, defaultValueComputed: CURRENT_TIMESTAMP }
        - createTable:
            tableName: match_candidate_result
            columns:
              - column: { name: id, type: BIGSERIAL, constraints: { primaryKey: true } }
              - column: { name: match_result_id, type: BIGINT, constraints: { nullable: false } }
              - column: { name: consultant_id, type: BIGINT, constraints: { nullable: false } }
              - column: { name: match_score, type: decimal(5,4), constraints: { nullable: false } }
              - column: { name: match_explanation, type: text }
              - column: { name: created_at, type: timestamptz, defaultValueComputed: CURRENT_TIMESTAMP }
        - addForeignKeyConstraint:
            baseTableName: project_match_result
            baseColumnNames: project_request_id
            referencedTableName: project_request
            referencedColumnNames: id
            constraintName: fk_match_project_request
        - addForeignKeyConstraint:
            baseTableName: match_candidate_result
            baseColumnNames: match_result_id
            referencedTableName: project_match_result
            referencedColumnNames: id
            constraintName: fk_match_candidate_result
        - addForeignKeyConstraint:
            baseTableName: match_candidate_result
            baseColumnNames: consultant_id
            referencedTableName: consultant
            referencedColumnNames: id
            constraintName: fk_match_consultant
        - createIndex:
            tableName: project_match_result
            indexName: idx_match_project_request
            columns:
              - column: { name: project_request_id }
        - createIndex:
            tableName: match_candidate_result
            indexName: idx_match_score_desc
            columns:
              - column: { name: match_result_id }
              - column: { name: match_score, order: desc }
EOF
```

#### REST Controller with Manual Trigger Endpoint

Create Kotlin REST controller:
- `MatchesController`: Handle HTTP requests for matching operations
- Endpoints:
  - `GET /api/matches/requests`: List all project requests
  - `POST /api/matches/requests/{id}/trigger`: Manual trigger matching
  - `GET /api/matches/requests/{id}/results`: Get match results
  - `GET /api/matches/requests/{id}/top`: Get top 10 matches
- Proper error handling and response formatting

#### Event Integration

Create event handling for automatic triggering:
- `ProjectRequestUploadedEvent`: Spring application event
- `ProjectRequestEventListener`: Listen for upload events
- Integration with existing project upload workflow

### Phase 3: Frontend Implementation

#### Update OpenAPI Specification

```warp-runnable-command
cp /Users/tandersen/git/cloudberries-candidate-match/candidate-match/openapi.yaml /Users/tandersen/git/cloudberries-candidate-match-web/openapi.yaml
```

#### Frontend Directory Structure

```warp-runnable-command
cd /Users/tandersen/git/cloudberries-candidate-match-web/src
mkdir -p api types components/matches pages/matches
```

#### TypeScript Types and API Client

Create TypeScript interfaces and API service:
- Type definitions matching backend DTOs
- API client functions for all matching endpoints
- Error handling and loading states
- Integration with existing API infrastructure

#### Enhanced Matches Page Component

Create React components for:
- `MatchesPage`: Main page with project request list
- `ProjectMatchCard`: Individual project request display
- `MatchTriggerButton`: Button to trigger matching manually
- `MatchResultsTable`: Display top 10 consultant matches
- `MatchScoreBadge`: Visual score representation
- Loading states and error handling

### Phase 4: Testing and Integration

#### Backend Testing

```warp-runnable-command
cd /Users/tandersen/git/cloudberries-candidate-match/candidate-match
sdk use java 21.0.7-tem
./mvnw clean test -Dspring.profiles.active=test
```

#### Frontend Testing

```warp-runnable-command
cd /Users/tandersen/git/cloudberries-candidate-match-web
npm install
npm run test
npm run build
```

### Phase 5: Local Development and Verification

#### Start Database

```warp-runnable-command
cd /Users/tandersen/git/cloudberries-candidate-match/candidate-match
docker-compose -f docker-compose-local.yaml up -d
```

#### Start Backend Application

```warp-runnable-command
cd /Users/tandersen/git/cloudberries-candidate-match/candidate-match
sdk use java 21.0.7-tem
./mvnw spring-boot:run -Dspring.profiles.active=local
```

#### Start Frontend Application

```warp-runnable-command
cd /Users/tandersen/git/cloudberries-candidate-match-web
npm run dev
```

#### Verification Steps

1. Navigate to `http://localhost:5173/matches`
2. Verify project requests are listed
3. Test manual "Trigger Matching" button functionality
4. Verify top 10 consultant matches display correctly
5. Test automatic matching on project upload
6. Check match scores and explanations

## Feature Specifications

### Automated Functionality

1. **Background Matching**: 
   - Triggers automatically when project request is uploaded
   - Runs asynchronously without blocking UI
   - Stores results for immediate retrieval

2. **Manual Trigger**: 
   - Frontend button calls controller endpoint
   - Provides immediate feedback to user
   - Shows loading state during computation

3. **Non-Interactive Processing**: 
   - No terminal prompts or user confirmations
   - Comprehensive logging for monitoring
   - Error handling without interruption

### Technical Implementation

1. **Clean Architecture**: 
   - Controller handles HTTP requests and responses
   - Service contains business logic and async operations
   - Repository manages data persistence and queries
   - Events enable loose coupling between components

2. **Database Design**: 
   - Normalized schema for match results
   - Proper indexes for query performance
   - Foreign key constraints for data integrity
   - Timestamp tracking for audit trails

3. **Frontend Integration**: 
   - Type-safe API integration
   - Responsive UI with loading states
   - Real-time updates after matching
   - Error handling and user feedback

4. **Performance Considerations**: 
   - Async processing prevents blocking
   - Database indexing for fast queries
   - Efficient batch processing of consultants
   - Caching of frequently accessed results

### API Endpoints

- `GET /api/matches/requests` - List project requests
- `POST /api/matches/requests/{id}/trigger` - Trigger matching
- `GET /api/matches/requests/{id}/results` - Get match results
- `GET /api/matches/requests/{id}/top` - Get top matches

### User Experience

1. **Matches Page Features**:
   - Table view of all project requests
   - "Trigger Matching" button for each project
   - Expandable section showing top 10 matches
   - Visual score indicators (green/yellow/red)
   - Match explanation tooltips

2. **Interaction Flow**:
   - User uploads project request → automatic matching begins
   - User visits /matches page → sees all projects
   - User clicks "Trigger Matching" → manual matching starts
   - User clicks "Show Results" → displays consultant matches
   - Results show scores, names, and AI explanations

---

**This implementation provides a comprehensive consultant matching system that operates seamlessly in both automatic and manual modes while following established architectural patterns and maintaining excellent user experience.**