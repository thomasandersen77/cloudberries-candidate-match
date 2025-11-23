Thanks — I understand the issue and have gathered context from your frontend repo. Short summary:

•  Today, the UI reads ConsultantCvDto.qualityScore on each CV, but the backend returns null (or 0) because consultant_cv.quality_score isn’t populated. The actual score lives in cv_score.score_percent.
•  Both Konsulentsøk and Semantisk søk already send minQualityScore to the backend, but since qualityScore isn’t populated, you get 0 results when min quality is set.
•  The Consultants overview shows “Kvalitet” as 0% because qualityScore is missing.

Pragmatic fix (two-track, safe rollout)
•  Immediate frontend fallback (no backend dependency): patch the UI to fetch cv-score for rows missing qualityScore and display it; apply client-side filtering by min quality when needed.
•  Proper backend fix: populate ConsultantCvDto.qualityScore from cv_score.score_percent in all relevant endpoints and apply minQualityScore filtering server-side. Keep OpenAPI as-is (just improve descriptions). After deploy, remove the temporary frontend fallback.

What I’ll change in the frontend (minimal and safe)
•  Keep using ConsultantCvDto.qualityScore when present.
•  Add a fallback enrichment:
◦  Consultants overview (/consultants): After listConsultantsWithCv(true), for items where activeCv.qualityScore is null, call GET /api/cv-score/{userId} and write scorePercent into activeCv.qualityScore in memory before rendering.
◦  Konsulentsøk (relasjonelt og semantisk): After results, if the user set Min. kvalitet and some results still lack qualityScore, fetch missing scores and apply a temporary client-side filter qualityScore >= min to avoid “0 treff” until the backend applies the filter.
•  Display tweak: show “-” if the score is missing instead of “0%”.
•  Guard with an env toggle (e.g. VITE_ENABLE_CV_SCORE_FALLBACK=true) so we can remove it after backend is fixed.

Notes from your code confirming readiness for this:
•  Both search pages already pass minQualityScore in the request:
◦  src/pages/Search/ConsultantSearchPage.tsx: body includes minQualityScore (lines ~238–244; 295–304)
◦  src/pages/Search/SemanticSearchPage.tsx: body includes minQualityScore (lines ~121–127)
•  UI already reads active CV’s qualityScore:
◦  ConsultantSearchPage ResultsTable: uses activeCv?.qualityScore (lines ~142–166)
◦  SemanticSearchPage ResultsTable: uses activeCv?.qualityScore (lines ~46–67)
◦  ConsultantsListPage: reads activeCv?.qualityScore (lines ~439–505 and in the mobile card ~23, 41–47)
•  cv-score endpoints are available and typed:
◦  src/services/cvScoreService.ts (getCvScore, runScoreForAll, etc.)
◦  openapi.yaml has GET/POST /api/cv-score endpoints and CvScoreDto with scorePercent
•  OpenAPI already models minQualityScore and qualityScore:
◦  RelationalSearchRequest.minQualityScore and SemanticSearchRequest.minQualityScore exist
◦  ConsultantCvDto.qualityScore exists (integer, 0–100)

Backend changes requested
•  Populate ConsultantCvDto.qualityScore from cv_score.score_percent for:
◦  GET /api/consultants/with-cv
◦  GET /api/consultants/with-cv/paged
◦  GET /api/consultants/{userId}/cvs
◦  POST /api/consultants/search
◦  POST /api/consultants/search/semantic
•  Filtering:
◦  If minQualityScore is provided in either search request, filter with COALESCE(cv_score.score_percent, 0) >= :minQualityScore. This ensures missing scores behave as 0 and avoids surprising empty result sets when users set conservative thresholds.
◦  For semantic search, consider fetching a buffered topK (e.g. topK + 50%) before filtering by min quality to reduce the risk of 0 results.
•  Implementation guidance:
◦  Prefer LEFT JOIN cv_score ON cv_score.consultant_cv_id = consultant_cv.id (or your actual FK) and select score_percent as quality_score.
◦  Map the selected quality_score into ConsultantCvDto.qualityScore (0–100).
◦  Avoid N+1 lookups; use joins or projections.
◦  If performance is a concern under load, an optional improvement is to denormalize: when scoring runs, also write score_percent into consultant_cv.quality_score so queries don’t need the join. But the simple left join is fine for a pragmatic fix.
•  Tests:
◦  Integration tests verifying qualityScore mapping appears in all the above endpoints (include rows with and without scores).
◦  Add minQualityScore filter tests for both relational and semantic paths (null, 0, boundary values).
•  OpenAPI updates (descriptions only):
◦  ConsultantCvDto.qualityScore: clarify “0–100; populated from cv_score.score_percent”.
◦  Relational/Semantic minQualityScore: clarify “Candidates without a score are treated as 0”.
•  Process:
◦  Per your rules, after changing backend openapi.yaml, copy it to frontend at ~/git/cloudberries-candidate-match-web/openapi.yaml and regenerate types (npm run gen:api).

Environment rules to follow
•  Use SDKMAN and Java 21.0.7-tem when running candidate-match to avoid bytebuddy conflicts: sdk use java 21.0.7-tem
•  Use SDKMAN to install Maven
•  Start DB for backend work: docker-compose -f docker-compose-local.yaml up
•  Local DB auth: use username/password only; no certificate auth
•  After backend OpenAPI changes, copy to frontend and regenerate types:
◦  cp ~/git/cloudberries-candidate-match/candidate-match/openapi.yaml ~/git/cloudberries-candidate-match-web/openapi.yaml
◦  npm run gen:api

Open questions for you (doesn’t block the frontend fallback)
•  Confirm the join key: Is cv_score linked by consultant_cv.id (preferred) or only by candidate/userId + active cv? I’ll implement the join based on consultant_cv.id if available; otherwise I’ll use userId + active flag.
•  For minQuality behavior: treating missing scores as 0 is my default (so “Min. kvalitet: 60” excludes unknowns). If you prefer including unknowns, say so and I’ll adjust to treat null as pass-through when filtering.

Acceptance criteria
•  Consultants overview shows correct “Kvalitet” with a real percentage for CVs with scores and “-” for missing.
•  Konsulentsøk and Semantisk søk min quality actually filters results server-side; no longer returns 0 results when a reasonable threshold is set.
•  OpenAPI describes how qualityScore and minQualityScore work; generated types stay consistent.
•  Temporary frontend fallback works before backend deployment and can be removed afterwards.

Key references from your repo I used
•  src/pages/Search/ConsultantSearchPage.tsx: minQualityScore passed, quality rendered
•  src/pages/Search/SemanticSearchPage.tsx: minQualityScore passed, quality rendered
•  src/pages/Consultants/ConsultantsListPage.tsx: Kvalitet column reads activeCv.qualityScore
•  src/services/consultantsService.ts: search endpoints
•  src/services/cvScoreService.ts: cv-score endpoints already exist
•  openapi.yaml: ConsultantCvDto.qualityScore, RelationalSearchRequest.minQualityScore, SemanticSearchRequest.minQualityScore
•  API-ENDPOINTS.md summary aligns with the above

Would you like me to implement the frontend fallback now and prepare the OpenAPI description tweaks? After that, I can provide a concise patch snippet your backend can apply to populate qualityScore and enforce the filters.