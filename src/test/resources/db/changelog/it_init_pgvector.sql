CREATE
EXTENSION IF NOT EXISTS vector;
CREATE TABLE IF NOT EXISTS cv_embedding
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    user_id
    VARCHAR
(
    255
) NOT NULL,
    cv_id VARCHAR
(
    255
) NOT NULL,
    provider VARCHAR
(
    50
) NOT NULL,
    model VARCHAR
(
    100
) NOT NULL,
    embedding VECTOR
(
    768
) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
CREATE UNIQUE INDEX IF NOT EXISTS uk_cv_embedding_user_cv_provider_model ON cv_embedding (user_id, cv_id, provider, model);
CREATE INDEX IF NOT EXISTS idx_cv_embedding_ivfflat ON cv_embedding USING ivfflat (embedding vector_l2_ops) WITH (lists = 100);