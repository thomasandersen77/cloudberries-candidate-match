Backend instructions (efficient, high-quality fix)
Goal
•  Populate ConsultantCvDto.cvs[*].qualityScore from cv_score.score_percent everywhere you return CVs.
•  Enforce minQualityScore in search endpoints so the frontend can remove the fallback.

Join mapping
•  Preferred key: cv_score.consultant_cv_id → consultant_cv.id
•  If that key doesn’t exist, coordinate a migration to add it. Otherwise, use a reliable join on (userId + active_cv) until schema is adjusted.

Implementation steps
1) Populate qualityScore on all CV-returning endpoints
•  Endpoints:
◦  GET /api/consultants/with-cv
◦  GET /api/consultants/with-cv/paged
◦  GET /api/consultants/{userId}/cvs
◦  POST /api/consultants/search
◦  POST /api/consultants/search/semantic
•  Repository query pattern (native SQL example):
◦  LEFT JOIN cv_score cs ON cs.consultant_cv_id = cv.id
◦  SELECT cs.score_percent AS quality_score
•  Map quality_score to ConsultantCvDto.qualityScore (Integer 0–100). When no row in cv_score, return null.
•  Avoid N+1: use join/projections instead of per-row lookups.

2) Apply minQualityScore in relational search
•  If request.minQualityScore != null:
◦  Add WHERE COALESCE(cs.score_percent, 0) >= :minQualityScore in the query, before pagination.
•  Keep counts correct by filtering before counting.
•  Return qualityScore as above.

3) Apply minQualityScore in semantic search
•  If request.minQualityScore != null:
◦  Option A (simple): Add the same COALESCE(cs.score_percent, 0) >= :minQualityScore predicate alongside your vector similarity query.
◦  Option B (buffer): Fetch topK + buffer (e.g., topK + 10 or +50%), filter by quality, then page. This reduces risk of empty pages.
•  Return qualityScore as above.

4) Indexing
•  Add an index if missing:
◦  CREATE INDEX IF NOT EXISTS idx_cv_score_consultant_cv_id ON cv_score(consultant_cv_id);

5) Tests
•  Integration tests for all endpoints verifying:
◦  qualityScore is populated when cv_score exists, null otherwise
◦  minQualityScore filters as expected (null/0/100 boundary)
•  Semantic search: verify quality filter still returns expected page sizes when reasonable min thresholds are used.
•  Avoid regressions by seeding data with:
◦  Multiple CVs per consultant
◦  CVs with and without scores

OpenAPI (backend)
•  Update descriptions (no structural changes):
◦  ConsultantCvDto.qualityScore: “Integer 0–100; populated from cv_score.score_percent”
◦  RelationalSearchRequest.minQualityScore: “Minimum CV quality score (0–100). Candidates without a score are treated as 0 for filtering.”
◦  SemanticSearchRequest.minQualityScore: same description
•  After backend openapi.yaml is updated, copy to frontend per your rule:
◦  cp ~/git/cloudberries-candidate-match/candidate-match/openapi.yaml ~/git/cloudberries-candidate-match-web/openapi.yaml
◦  npm run gen:api

Performance note
•  If the LEFT JOIN proves costly at scale, consider denormalization:
◦  On scoring runs, also write score_percent to consultant_cv.quality_score.
◦  Then read directly from consultant_cv (no join) for hot paths. Keep cv_score as the audit/source-of-truth.

Rollout plan
•  Step 1: Ship the current frontend fallback (done; enabled by default).
•  Step 2: Deploy backend with mapping and server-side minQuality filtering.
•  Step 3: Set VITE_ENABLE_CV_SCORE_FALLBACK=false and redeploy frontend, or remove the fallback code en
