# AI Ticketing Bootstrap

This is the initial implementation for the AI ticketing plan:

- Customer asks -> AI answers
- If AI cannot solve, handoff to L1 support
- If L1 cannot solve, escalate to L2 technical support
- High-value solved tickets can be ingested to knowledge base

## Current Scope

- Spring Boot 3.5 + Java 21 project scaffold
- Spring AI model config placeholders (`qwen-plus`, `text-embedding-v3`)
- Ticket lifecycle APIs
- Bootstrap RAG orchestrator service with:
  - hybrid retrieval (pgvector SQL + keyword SQL in postgres mode)
  - RRF fusion (`k=60`)
  - reranker timeout fallback (800ms -> use RRF ranking)
- Hit-rate metrics endpoint
- Knowledge ingest endpoint
- Knowledge base persisted in database (H2 by default)
- Built-in sample knowledge base for local testing
- Integration test set (`src/test/resources/rag-testset.json`)

## API Endpoints

- `POST /api/tickets/ask`
- `POST /api/tickets/ask/stream` (SSE stream)
- `POST /api/tickets/{id}/handoff/l1`
- `POST /api/tickets/{id}/handoff/l2`
- `POST /api/tickets/{id}/resolve`
- `GET /api/tickets`
- `GET /api/tickets/metrics/hitrate`
- `POST /api/knowledge/ingest`
- `GET /api/knowledge/titles`
- `POST /api/knowledge/attachments/upload` (multipart, params: `source`, `file`)
- `GET /api/knowledge/attachments?source=...`
- `GET /api/attachments/{id}`
- `GET /api/prompts/active?key=ticket.system`
- `GET /api/prompts/history?key=ticket.system`
- `POST /api/prompts/versions`
- `POST /api/prompts/rollback`
- `GET /api/system/status`

## Next Implementation Tasks

1. Replace in-memory retrieval with PostgreSQL + PGVector + GIN SQL retrieval.
2. Add permission hard-filtering in repository queries (`allowedKbIds`).
3. Persist knowledge chunks and retrieval logs.
4. Add query-rewrite and source-grounding optimization against real model APIs.

## Local Run Without PostgreSQL

You can run everything with default config (H2 in-memory DB).  
No local PostgreSQL or PGVector is required for current development stage.

## Switch To PostgreSQL Later

1. Start PostgreSQL and create `ragask` database.
2. Apply script: `src/main/resources/sql/postgres-pgvector-init.sql`
3. Run app with postgres profile:
   - `--spring.profiles.active=postgres`
4. Update `src/main/resources/application-postgres.yml` credentials if needed.

### PostgreSQL profile quick start (Windows cmd)

```bat
set PG_USER=postgres
set PG_PASSWORD=your_password
set PG_URL=jdbc:postgresql://localhost:5432/ragask
```

Then start:

```bat
"D:\Compiler\Lang\maven\apache-maven-3.9.14\bin\mvn.cmd" spring-boot:run -Dspring-boot.run.profiles=postgres
```

Notes:

- In `postgres` mode, retrieval runs with SQL full-text + Java candidate scoring, then RRF + rerank fallback.
- `pgvector` route now uses true SQL vector search (`embedding <=> query_vector`) for TopK.
- Embedding is online-first:
  - if `spring.ai.openai.embedding` is configured and reachable, vectors come from the remote embedding API
  - otherwise it falls back to deterministic local vectors
- Generation is real DeepSeek API when `DEEPSEEK_API_KEY` is set.
- Without API key, response falls back to local deterministic answer generation.

## Frontend Demo

A simple React page is served by Spring Boot at:

- `http://localhost:8080/`

The page supports:

- ask ticket question
- runtime DeepSeek API key input and browser localStorage save
- show AI response, citations, ticket status
- show attachments for high-confidence hits (pdf/docx/png/jpeg)
- show hit-rate metrics
- list knowledge titles
- upload attachments to a knowledge item (bind by `source`)

## DeepSeek Setup

```bat
set DEEPSEEK_API_KEY=your_key
set DEEPSEEK_MODEL=deepseek-v4-pro
```

Optional:

```bat
set REDIS_HOST=localhost
set REDIS_PORT=6379
set OPENAI_API_KEY=your_embedding_provider_key
set HANDOFF_L1_URL=http://localhost:9001/api/l1/tickets
set HANDOFF_L2_URL=http://localhost:9002/api/l2/tickets
```

Then run:

```bat
"D:\Compiler\Lang\maven\apache-maven-3.9.14\bin\mvn.cmd" spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Run Tests

Use your Maven path:

`D:\Compiler\Lang\maven\apache-maven-3.9.14\bin\mvn.cmd test`
