-- Run manually after PostgreSQL + pgvector is installed.
-- Example: psql -U postgres -d ragask -f postgres-pgvector-init.sql

CREATE EXTENSION IF NOT EXISTS vector;

-- Future-ready table for real vector retrieval.
CREATE TABLE IF NOT EXISTS knowledge_chunks (
    id BIGSERIAL PRIMARY KEY,
    doc_id BIGINT UNIQUE,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    source VARCHAR(500) NOT NULL,
    embedding VECTOR(1536),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_knowledge_chunks_tenant ON knowledge_chunks (tenant_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_chunks_embedding
    ON knowledge_chunks USING hnsw (embedding vector_cosine_ops);
