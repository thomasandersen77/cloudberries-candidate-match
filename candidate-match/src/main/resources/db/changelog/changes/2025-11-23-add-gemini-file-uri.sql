-- Adds gemini_file_uri column to consultant_cv table for caching Gemini Files API uploads
-- This prevents re-uploading the same CV multiple times to Gemini
-- Files expire after 48 hours on Gemini side, but cache remains for quick re-upload detection

ALTER TABLE consultant_cv
    ADD COLUMN IF NOT EXISTS gemini_file_uri VARCHAR(512);

COMMENT ON COLUMN consultant_cv.gemini_file_uri IS
    'Cached URI from Gemini Files API upload. Format: https://generativelanguage.googleapis.com/v1beta/files/{file_id}. Files expire after 48 hours.';
