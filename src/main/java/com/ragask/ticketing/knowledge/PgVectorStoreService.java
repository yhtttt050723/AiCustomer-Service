package com.ragask.ticketing.knowledge;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PgVectorStoreService {

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;
    private final String retrievalBackend;
    private final AtomicBoolean vectorReady = new AtomicBoolean(false);
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public PgVectorStoreService(
            JdbcTemplate jdbcTemplate,
            EmbeddingService embeddingService,
            @Value("${rag.retrieval.backend:memory}") String retrievalBackend
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingService = embeddingService;
        this.retrievalBackend = retrievalBackend;
    }

    public void indexDocument(KnowledgeDocument document) {
        if (!isPostgresBackend()) {
            return;
        }
        ensureSchema();
        float[] vector = embeddingService.embed(document.getTitle() + " " + document.getContent());
        String vectorLiteral = embeddingService.toPgVectorLiteral(vector);
        jdbcTemplate.update(
                """
                INSERT INTO knowledge_chunks (doc_id, tenant_id, title, content, source, embedding)
                VALUES (?, ?, ?, ?, ?, CAST(? AS vector))
                ON CONFLICT (doc_id) DO UPDATE
                SET tenant_id = EXCLUDED.tenant_id,
                    title = EXCLUDED.title,
                    content = EXCLUDED.content,
                    source = EXCLUDED.source,
                    embedding = EXCLUDED.embedding
                """,
                document.getId(),
                document.getTenantId(),
                document.getTitle(),
                document.getContent(),
                document.getSource(),
                vectorLiteral
        );
    }

    public List<KnowledgeService.KnowledgeChunk> vectorSearch(String query, int topK) {
        if (!isPostgresBackend()) {
            return List.of();
        }
        ensureSchema();
        String queryVector = embeddingService.toPgVectorLiteral(embeddingService.embed(query));
        return jdbcTemplate.query(
                """
                SELECT id, title, content, source
                FROM knowledge_chunks
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT ?
                """,
                (rs, rowNum) -> new KnowledgeService.KnowledgeChunk(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getString("source")
                ),
                queryVector,
                topK
        );
    }

    public List<KnowledgeService.KnowledgeChunk> keywordSearch(String query, int topK) {
        if (!isPostgresBackend()) {
            return List.of();
        }
        ensureSchema();
        return jdbcTemplate.query(
                """
                SELECT id, title, content, source
                FROM knowledge_chunks
                WHERE to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(content, ''))
                      @@ plainto_tsquery('simple', ?)
                ORDER BY ts_rank(
                           to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(content, '')),
                           plainto_tsquery('simple', ?)
                         ) DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new KnowledgeService.KnowledgeChunk(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getString("source")
                ),
                query,
                query,
                topK
        );
    }

    private void ensureSchema() {
        if (initialized.get()) {
            return;
        }
        synchronized (this) {
            if (initialized.get()) {
                return;
            }
            try {
                jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
                jdbcTemplate.execute(
                        """
                        CREATE TABLE IF NOT EXISTS knowledge_chunks (
                            id BIGSERIAL PRIMARY KEY,
                            doc_id BIGINT UNIQUE,
                            tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
                            title VARCHAR(255) NOT NULL,
                            content TEXT NOT NULL,
                            source VARCHAR(500) NOT NULL,
                            embedding VECTOR(1536),
                            created_at TIMESTAMP NOT NULL DEFAULT NOW()
                        )
                        """
                );
                jdbcTemplate.execute(
                        "CREATE INDEX IF NOT EXISTS idx_knowledge_chunks_embedding ON knowledge_chunks USING hnsw (embedding vector_cosine_ops)"
                );
                jdbcTemplate.execute(
                        "CREATE INDEX IF NOT EXISTS idx_knowledge_chunks_tenant ON knowledge_chunks (tenant_id)"
                );
                vectorReady.set(true);
            } catch (Exception e) {
                vectorReady.set(false);
                throw e;
            } finally {
                initialized.set(true);
            }
        }
    }

    private boolean isPostgresBackend() {
        return "postgres".equalsIgnoreCase(retrievalBackend);
    }

    public boolean pgvectorReady() {
        if (!isPostgresBackend()) {
            return false;
        }
        if (!initialized.get()) {
            try {
                ensureSchema();
            } catch (Exception ignored) {
                return false;
            }
        }
        return vectorReady.get();
    }
}
