-- Initialize pgvector extension for standalone Testcontainers PostgreSQL
CREATE EXTENSION IF NOT EXISTS vector;

-- The embeddings table will be created automatically by Spring AI pgvector store
-- This script just ensures the vector extension is available for standalone mode
