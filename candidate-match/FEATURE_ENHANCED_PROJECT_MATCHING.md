# Feature: Enhanced Project Request Matching with CV Quality Integration

## Overview
This feature enhances the existing Project Request upload and matching pipeline to intelligently select and analyze top consultants based on both skill matching AND CV quality. The system will automatically compute AI-powered match scores for at least 5 consultants when a new project is uploaded.

## Business Requirements

### Core Functionality
When a project request is uploaded via `/api/project-requests/upload`:
1. **Extract required skills** from the project description (already implemented via `ProjectRequestAnalysisService`)
2. **Select candidate consultants** (minimum 5, recommended 10-15) based on:
   - Skill match relevance (primary factor)
   - CV quality score (secondary factor)
   - CV completeness (consultant must have CV data available)
3. **Compute AI match scores** for each selected consultant using Gemini
4. **Persist match results** to database for retrieval
5. **Display results** in frontend table with clickable rows to view detailed matches

### User Experience
In the "Matches" page (`/matches`):
- Display a **table of all project requests** with columns:
  - Project title/description (truncated)
  - Customer name
  - Upload/creation date
  - Status indicator (matches computed/pending)
  - Number of top matches found
- Each **row is clickable/expandable** to show:
  - Top matched consultants (ranked by score)
  - Match explanations from AI
  - Consultant details (name, userId, cvId)
  - Option to recompute matches

## Technical Implementation Plan

### Phase 1: Backend - Enhanced Candidate Selection Logic

#### 1.1 Add CV Quality Score Integration to Candidate Selection
**File**: `ProjectMatchingServiceImpl.kt`
**Method**: `getConsultantsForMatching()`

**Current logic**:
```kotlin
private fun getConsultantsForMatching(projectRequest): List<ConsultantWithCvDto> {
    val requiredSkills = projectRequest.requiredSkills?.map { it.name } ?: emptyList()
    
    return if (requiredSkills.isNotEmpty()) {
        consultantWithCvService.getTopConsultantsBySkills(requiredSkills, limit = 30)
    } else {
        consultantWithCvService.getAllConsultantsWithCvs(onlyActiveCv = true).take(20)
    }
}
```

**Enhanced logic needed**:
```kotlin
private fun getConsultantsForMatching(projectRequest): List<ConsultantWithCvDto> {
    val requiredSkills = projectRequest.requiredSkills?.map { it.name } ?: emptyList()
    
    // Get skill-matched consultants (expanded pool)
    val skillMatched = if (requiredSkills.isNotEmpty()) {
        consultantWithCvService.getTopConsultantsBySkills(requiredSkills, limit = 50)
    } else {
        consultantWithCvService.getAllConsultantsWithCvs(onlyActiveCv = true).take(30)
    }
    
    // Score consultants by combined: skill_match + cv_quality
    val scoredConsultants = scoreConsultantsByCombinedRelevance(
        consultants = skillMatched,
        requiredSkills = requiredSkills,
        minCandidates = 10,
        maxCandidates = 15
    )
    
    return scoredConsultants
}
```

#### 1.2 Create Consultant Scoring Service
**New file**: `ConsultantScoringService.kt`
**Location**: `service/matching/ConsultantScoringService.kt`

**Responsibilities**:
- Fetch CV quality scores for consultants (from existing `/api/cv-score/{candidateId}` endpoint)
- Combine skill matching score with CV quality score
- Apply weighting: `combined_score = (0.7 * skill_match) + (0.3 * cv_quality)`
- Sort and return top N consultants

**Key methods**:
```kotlin
@Service
class ConsultantScoringService(
    private val consultantRepository: ConsultantRepository,
    private val cvScoreRepository: CvScoreRepository // You may need to create this
) {
    
    /**
     * Scores consultants by combining skill match and CV quality.
     * 
     * @param consultants Pre-filtered consultants (from skill matching)
     * @param requiredSkills Skills required for the project
     * @param minCandidates Minimum number of candidates to return (default: 5)
     * @param maxCandidates Maximum number of candidates to return (default: 15)
     * @return Ranked list of consultants by combined score
     */
    fun scoreConsultantsByCombinedRelevance(
        consultants: List<ConsultantWithCvDto>,
        requiredSkills: List<String>,
        minCandidates: Int = 5,
        maxCandidates: Int = 15
    ): List<ConsultantWithCvDto> {
        
        // Step 1: Calculate skill match scores (0.0 to 1.0)
        val skillScores = consultants.map { consultant ->
            val score = calculateSkillMatchScore(consultant, requiredSkills)
            consultant to score
        }
        
        // Step 2: Fetch CV quality scores for all consultants
        val cvQualityScores = fetchCvQualityScores(consultants.mapNotNull { it.id })
        
        // Step 3: Combine scores with weighting
        val combinedScores = skillScores.map { (consultant, skillScore) ->
            val cvQuality = cvQualityScores[consultant.id] ?: 0.0
            val combinedScore = (SKILL_WEIGHT * skillScore) + (CV_QUALITY_WEIGHT * cvQuality)
            
            Triple(consultant, combinedScore, cvQuality)
        }
        
        // Step 4: Sort by combined score and take top N
        val ranked = combinedScores
            .filter { it.second > MIN_THRESHOLD_SCORE } // Only consultants with some relevance
            .sortedByDescending { it.second }
        
        // Ensure minimum candidates
        val selected = if (ranked.size < minCandidates) {
            logger.warn { "Only ${ranked.size} consultants meet threshold, expected at least $minCandidates" }
            ranked.take(minCandidates.coerceAtMost(consultants.size))
        } else {
            ranked.take(maxCandidates)
        }
        
        logger.info { 
            "Selected ${selected.size} consultants for matching. " +
            "Score range: ${selected.lastOrNull()?.second?.format(2)} to ${selected.firstOrNull()?.second?.format(2)}"
        }
        
        return selected.map { it.first }
    }
    
    /**
     * Calculates skill match score as percentage of required skills covered.
     */
    private fun calculateSkillMatchScore(
        consultant: ConsultantWithCvDto,
        requiredSkills: List<String>
    ): Double {
        if (requiredSkills.isEmpty()) return 0.5 // Default score if no skills specified
        
        val consultantSkills = consultant.skills.map { it.uppercase() }.toSet()
        val required = requiredSkills.map { it.uppercase() }.toSet()
        
        val matchCount = consultantSkills.intersect(required).size
        return matchCount.toDouble() / required.size
    }
    
    /**
     * Fetches CV quality scores from database for batch of consultants.
     */
    private fun fetchCvQualityScores(consultantIds: List<Long>): Map<Long, Double> {
        // Query cv_score table (or equivalent) for quality scores
        // This should use existing CvScore infrastructure
        // Return normalized scores (0.0 to 1.0)
        
        return consultantRepository.findAllById(consultantIds)
            .mapNotNull { consultant ->
                val cvScore = cvScoreRepository.findByConsultantId(consultant.id)
                consultant.id?.let { id -> 
                    id to (cvScore?.normalizedScore ?: DEFAULT_CV_SCORE)
                }
            }
            .toMap()
    }
    
    companion object {
        private const val SKILL_WEIGHT = 0.7
        private const val CV_QUALITY_WEIGHT = 0.3
        private const val MIN_THRESHOLD_SCORE = 0.2 // Minimum 20% combined relevance
        private const val DEFAULT_CV_SCORE = 0.5 // Default if no CV score exists
    }
}
```

#### 1.3 Update ProjectMatchingServiceImpl
**File**: `ProjectMatchingServiceImpl.kt`

**Changes needed**:
1. Inject `ConsultantScoringService`
2. Update `getConsultantsForMatching()` to use scoring service
3. Ensure minimum 5 consultants are always selected (even with low scores)
4. Add logging for selected consultants and their scores

**Code changes**:
```kotlin
@Service
class ProjectMatchingServiceImpl(
    private val projectMatchResultRepository: ProjectMatchResultRepository,
    private val matchCandidateResultRepository: MatchCandidateResultRepository,
    private val projectRequestRepository: ProjectRequestRepository,
    private val consultantWithCvService: ConsultantWithCvService,
    private val candidateMatchingService: CandidateMatchingService,
    private val consultantScoringService: ConsultantScoringService // NEW
) : ProjectMatchingService {
    
    // ... existing code ...
    
    private fun getConsultantsForMatching(
        projectRequest: ProjectRequestEntity
    ): List<ConsultantWithCvDto> {
        
        val requiredSkills = projectRequest.requiredSkills?.map { it.name } ?: emptyList()
        
        logger.info { 
            "Selecting consultants for project ${projectRequest.id}. Required skills: $requiredSkills" 
        }
        
        // Get expanded pool of consultants based on skills
        val candidatePool = if (requiredSkills.isNotEmpty()) {
            consultantWithCvService.getTopConsultantsBySkills(requiredSkills, limit = 50)
        } else {
            consultantWithCvService.getAllConsultantsWithCvs(onlyActiveCv = true).take(30)
        }
        
        if (candidatePool.isEmpty()) {
            logger.warn { "No consultants available for matching" }
            return emptyList()
        }
        
        // Score and rank consultants by combined skill + CV quality
        val selectedConsultants = consultantScoringService.scoreConsultantsByCombinedRelevance(
            consultants = candidatePool,
            requiredSkills = requiredSkills,
            minCandidates = 5,
            maxCandidates = 15
        )
        
        logger.info { 
            "Selected ${selectedConsultants.size} consultants for AI matching: " +
            selectedConsultants.take(5).joinToString { it.name }
        }
        
        return selectedConsultants
    }
}
```

### Phase 2: Backend - OpenAPI Contract Updates

#### 2.1 Update OpenAPI specification
**File**: `openapi.yaml`

**Add/update endpoints**:

```yaml
# Existing endpoint - ensure it returns complete data
/api/matches/projects:
  get:
    summary: List all project requests with match status
    tags:
      - Matches
    responses:
      '200':
        description: List of project requests
        content:
          application/json:
            schema:
              type: array
              items:
                $ref: '#/components/schemas/ProjectRequestSummary'

# Existing endpoint - ensure it's well documented
/api/matches/projects/{projectId}:
  get:
    summary: Get top matches for a specific project
    tags:
      - Matches
    parameters:
      - name: projectId
        in: path
        required: true
        schema:
          type: integer
          format: int64
    responses:
      '200':
        description: Top consultant matches for project
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/MatchTop10Response'
      '404':
        description: Project not found or no matches computed

components:
  schemas:
    ProjectRequestSummary:
      type: object
      properties:
        id:
          type: integer
          format: int64
        title:
          type: string
          description: Truncated project description
        customerName:
          type: string
        createdAt:
          type: string
          format: date-time
        hasMatches:
          type: boolean
          description: Whether matches have been computed
        matchCount:
          type: integer
          description: Number of matches found
          
    MatchTop10Response:
      type: object
      properties:
        projectRequestId:
          type: integer
          format: int64
        projectTitle:
          type: string
        totalMatches:
          type: integer
        matches:
          type: array
          items:
            $ref: '#/components/schemas/MatchCandidate'
        lastUpdated:
          type: string
          format: date-time
          
    MatchCandidate:
      type: object
      properties:
        consultantId:
          type: integer
          format: int64
        consultantName:
          type: string
        userId:
          type: string
        cvId:
          type: string
          nullable: true
        matchScore:
          type: number
          format: decimal
          description: Match score between 0 and 1
        matchExplanation:
          type: string
          nullable: true
        createdAt:
          type: string
          format: date-time
```

#### 2.2 Sync OpenAPI to Frontend
After updating `openapi.yaml`:

```bash
# From backend directory
cp /Users/tandersen/git/cloudberries-candidate-match/candidate-match/openapi.yaml \
   /Users/tandersen/git/cloudberries-candidate-match-web/openapi.yaml

# From frontend directory
cd /Users/tandersen/git/cloudberries-candidate-match-web
npm run gen:api
```

### Phase 3: Frontend - Enhanced Matches Page

#### 3.1 Create Project Requests Table Component
**New file**: `src/pages/Matches/ProjectRequestsTable.tsx`

**Purpose**: Display all project requests in a table with clickable rows

```typescript
import React, { useState } from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Collapse,
  Box,
  Typography,
  Chip,
  CircularProgress
} from '@mui/material';
import {
  KeyboardArrowDown as ArrowDownIcon,
  KeyboardArrowUp as ArrowUpIcon,
  CheckCircle as CheckIcon,
  HourglassEmpty as PendingIcon
} from '@mui/icons-material';
import { ProjectRequestSummaryDto } from '../../types/generated'; // From OpenAPI
import MatchResultsTable from '../../components/matches/MatchResultsTable';

interface ProjectRequestsTableProps {
  projectRequests: ProjectRequestSummaryDto[];
  onRowClick: (projectId: number) => void;
  expandedRowId: number | null;
  matchesData: Record<number, MatchCandidate[]>;
  loadingMatches: Record<number, boolean>;
}

const ProjectRequestsTable: React.FC<ProjectRequestsTableProps> = ({
  projectRequests,
  onRowClick,
  expandedRowId,
  matchesData,
  loadingMatches
}) => {
  
  return (
    <TableContainer component={Paper} elevation={2}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell width={50} />
            <TableCell>Customer</TableCell>
            <TableCell>Project Description</TableCell>
            <TableCell align="center">Status</TableCell>
            <TableCell align="center">Matches</TableCell>
            <TableCell align="right">Created</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {projectRequests.map((project) => (
            <ProjectRequestRow
              key={project.id}
              project={project}
              isExpanded={expandedRowId === project.id}
              matches={matchesData[project.id]}
              loading={loadingMatches[project.id]}
              onRowClick={() => onRowClick(project.id)}
            />
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
};

interface ProjectRequestRowProps {
  project: ProjectRequestSummaryDto;
  isExpanded: boolean;
  matches?: MatchCandidate[];
  loading?: boolean;
  onRowClick: () => void;
}

const ProjectRequestRow: React.FC<ProjectRequestRowProps> = ({
  project,
  isExpanded,
  matches,
  loading,
  onRowClick
}) => {
  
  const hasMatches = matches && matches.length > 0;
  const matchCount = matches?.length ?? 0;
  
  return (
    <>
      <TableRow
        hover
        onClick={onRowClick}
        sx={{
          cursor: 'pointer',
          '&:hover': { backgroundColor: 'action.hover' }
        }}
      >
        <TableCell>
          <IconButton size="small">
            {isExpanded ? <ArrowUpIcon /> : <ArrowDownIcon />}
          </IconButton>
        </TableCell>
        
        <TableCell>
          <Typography variant="body2" fontWeight={500}>
            {project.customerName}
          </Typography>
        </TableCell>
        
        <TableCell>
          <Typography variant="body2" color="text.secondary">
            {project.title ?? 'No description'}
          </Typography>
        </TableCell>
        
        <TableCell align="center">
          {loading ? (
            <CircularProgress size={20} />
          ) : hasMatches ? (
            <Chip
              icon={<CheckIcon />}
              label="Computed"
              size="small"
              color="success"
              variant="outlined"
            />
          ) : (
            <Chip
              icon={<PendingIcon />}
              label="Pending"
              size="small"
              color="warning"
              variant="outlined"
            />
          )}
        </TableCell>
        
        <TableCell align="center">
          {hasMatches ? (
            <Chip label={matchCount} size="small" color="primary" />
          ) : (
            <Typography variant="caption" color="text.disabled">
              -
            </Typography>
          )}
        </TableCell>
        
        <TableCell align="right">
          <Typography variant="caption" color="text.secondary">
            {new Date(project.createdAt).toLocaleDateString()}
          </Typography>
        </TableCell>
      </TableRow>
      
      <TableRow>
        <TableCell style={{ paddingBottom: 0, paddingTop: 0 }} colSpan={6}>
          <Collapse in={isExpanded} timeout="auto" unmountOnExit>
            <Box sx={{ margin: 2 }}>
              <Typography variant="h6" gutterBottom component="div">
                Top Consultant Matches
              </Typography>
              <MatchResultsTable matches={matches} loading={loading} />
            </Box>
          </Collapse>
        </TableCell>
      </TableRow>
    </>
  );
};

export default ProjectRequestsTable;
```

#### 3.2 Update Matches Page to Use Table
**File**: `src/pages/Matches/MatchesPage.tsx`

**Changes**:
1. Replace card-based layout with `ProjectRequestsTable`
2. Keep existing loading/error states
3. Maintain clickable row behavior
4. Auto-load matches when row is expanded

```typescript
// In MatchesPage.tsx
import ProjectRequestsTable from './ProjectRequestsTable';

// Inside component JSX, replace the card mapping section:
<ProjectRequestsTable
  projectRequests={state.projectRequests}
  onRowClick={handleToggleExpand}
  expandedRowId={state.expandedProjectId}
  matchesData={state.matches}
  loadingMatches={state.loading}
/>
```

### Phase 4: Testing & Validation

#### 4.1 Backend Unit Tests
**Create**: `ConsultantScoringServiceTest.kt`

Test cases:
- Consultant selection with various skill combinations
- CV quality score integration
- Minimum candidate guarantee (always return at least 5)
- Score weighting calculation
- Edge cases: no consultants, no skills, all low scores

#### 4.2 Backend Integration Tests
**Update**: `ProjectMatchingServiceImplIntegrationTest.kt`

Test scenarios:
- Upload project → verify at least 5 consultants are analyzed
- Verify consultants have CV quality scores
- Verify Gemini AI is called for each consultant
- Verify match results are persisted correctly
- Test recomputation with `forceRecompute=true`

#### 4.3 Frontend Testing
**Manual testing checklist**:
- [ ] Upload new project request via `/project-requests/upload`
- [ ] Verify automatic matching is triggered
- [ ] Navigate to `/matches` page
- [ ] Verify project appears in table
- [ ] Click project row → verify expansion shows matches
- [ ] Verify match scores are displayed correctly
- [ ] Verify consultant names are clickable/viewable
- [ ] Test multiple projects in table
- [ ] Test "Compute Matches" button for recomputation

### Phase 5: Performance Considerations

#### 5.1 Optimization Strategies
1. **Batch AI calls**: Already implemented with coroutines (5 concurrent requests)
2. **Cache CV quality scores**: Add caching layer for frequently accessed scores
3. **Async processing**: Maintain async matching to avoid blocking uploads
4. **Database indexing**: Ensure indexes on:
   - `project_match_result.project_request_id`
   - `match_candidate_result.match_result_id`
   - `match_candidate_result.match_score` (for sorting)
   - `cv_score.consultant_id`

#### 5.2 Rate Limiting
- Gemini API has rate limits
- Current implementation uses batch size of 5 concurrent requests
- Consider adding exponential backoff for API failures
- Add retry logic with jitter

### Phase 6: Configuration & Deployment

#### 6.1 Configuration Properties
**File**: `application.yml` (or `application-local.yml`)

```yaml
matching:
  consultant-selection:
    min-candidates: 5
    max-candidates: 15
    skill-weight: 0.7
    cv-quality-weight: 0.3
    min-threshold-score: 0.2
  
  ai-analysis:
    batch-size: 5
    timeout-seconds: 30
    retry-attempts: 3
```

#### 6.2 Feature Flags (Optional)
Consider adding feature flag for gradual rollout:
```yaml
features:
  enhanced-matching-enabled: true
  use-cv-quality-scoring: true
```

## Success Criteria

### Functional Requirements ✅
- [ ] Minimum 5 consultants are selected for each project upload
- [ ] Consultants are selected based on skill match + CV quality
- [ ] AI analysis (Gemini) computes match score for each consultant
- [ ] Match results are persisted to database
- [ ] Frontend displays project requests in table format
- [ ] Table rows are clickable to expand and show matches
- [ ] Matches are sorted by score (highest first)

### Performance Requirements ✅
- [ ] Matching computation completes within 60 seconds for 15 consultants
- [ ] Page load time < 2 seconds for matches table
- [ ] API responses are properly cached where appropriate

### Quality Requirements ✅
- [ ] Unit test coverage > 80% for new services
- [ ] Integration tests cover end-to-end matching flow
- [ ] Error handling for AI failures (graceful degradation)
- [ ] Logging at appropriate levels (INFO for key events, DEBUG for details)

## Rollout Plan

### Phase 1: Backend (Week 1)
1. Implement `ConsultantScoringService`
2. Update `ProjectMatchingServiceImpl`
3. Add unit tests
4. Add integration tests
5. Update OpenAPI spec

### Phase 2: Frontend (Week 2)
1. Sync OpenAPI and regenerate types
2. Create `ProjectRequestsTable` component
3. Update `MatchesPage` to use table
4. Manual testing
5. Fix bugs and polish UI

### Phase 3: Validation & Deployment (Week 3)
1. End-to-end testing in staging
2. Performance testing with realistic data
3. User acceptance testing
4. Deploy to production
5. Monitor logs and metrics

## Open Questions & Decisions Needed

1. **CV Quality Score Availability**: 
   - Are CV scores already computed for all consultants?
   - If not, should we trigger CV score computation on upload?
   
2. **Fallback Strategy**:
   - What if fewer than 5 consultants are available?
   - Should we relax skill matching constraints?

3. **UI/UX**:
   - Should the table automatically expand the most recent project?
   - Should there be a "View CV" link for each consultant?

4. **Notifications**:
   - Should users be notified when matching completes?
   - Email, in-app, or both?

## References

### Existing Code
- `ProjectRequestController.kt` - Upload endpoint
- `ProjectMatchingServiceImpl.kt` - Matching logic
- `ConsultantWithCvService.kt` - Consultant retrieval
- `CandidateMatchingService.kt` - AI matching
- `ProjectMatchingPage.tsx` - Frontend matches page

### API Endpoints
- `POST /api/project-requests/upload` - Upload project
- `GET /api/matches/projects` - List projects
- `GET /api/matches/projects/{id}` - Get matches for project
- `POST /api/matches/projects/{id}/trigger` - Trigger matching
- `GET /api/cv-score/{candidateId}` - Get CV quality score

### Database Tables
- `project_request` - Project requests (domain table)
- `project_match_result` - Match computation results
- `match_candidate_result` - Individual consultant matches
- `cv_score` - CV quality scores (verify table name)

---

**Document Version**: 1.0  
**Created**: 2025-11-21  
**Author**: Warp AI Assistant  
**Status**: Ready for Implementation
