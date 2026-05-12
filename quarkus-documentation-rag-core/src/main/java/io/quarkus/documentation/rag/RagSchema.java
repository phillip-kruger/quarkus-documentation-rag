package io.quarkus.documentation.rag;

public final class RagSchema {

    public static final String TABLE_NAME = "rag_documents";
    public static final int EMBEDDING_DIMENSION = 384;
    public static final int INDEX_LIST_SIZE = 100;

    public static final String META_INF_PATH = "META-INF/quarkus-rag.sql";

    public static final String CREATE_EXTENSION_DDL = "CREATE EXTENSION IF NOT EXISTS vector;";

    public static final String CREATE_TABLE_DDL = """
            CREATE TABLE IF NOT EXISTS %s (
                embedding_id UUID PRIMARY KEY,
                embedding vector(%d),
                text TEXT,
                metadata JSONB
            );""".formatted(TABLE_NAME, EMBEDDING_DIMENSION);

    public static final String CREATE_INDEX_DDL = """
            CREATE INDEX IF NOT EXISTS idx_rag_embedding ON %s
                USING ivfflat (embedding vector_cosine_ops) WITH (lists = %d);"""
            .formatted(TABLE_NAME, INDEX_LIST_SIZE);

    public static final String SCHEMA_PREAMBLE = String.join("\n",
            "-- Quarkus Documentation RAG schema",
            CREATE_EXTENSION_DDL,
            "",
            CREATE_TABLE_DDL,
            "",
            CREATE_INDEX_DDL,
            "");

    private RagSchema() {
    }
}
