package io.quarkus.documentation.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.documentation.rag.SqlFragmentWriter.ChunkWithEmbedding;

class SqlFragmentWriterTest {

    @Test
    void writesValidSqlFragment(@TempDir Path tempDir) throws IOException {
        SqlFragmentWriter writer = new SqlFragmentWriter();
        Path output = tempDir.resolve("test.sql");

        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", "quarkus-rest");
        metadata.put("title", "REST Guide");

        float[] embedding = { 0.1f, 0.2f, 0.3f };
        ChunkWithEmbedding chunk = new ChunkWithEmbedding("Hello world", embedding, metadata);

        writer.write(output, "quarkus-rest", "3.21.0", List.of(chunk));

        String sql = Files.readString(output);
        assertThat(sql).contains("-- quarkus-rag fragment: quarkus-rest 3.21.0");
        assertThat(sql).contains("DELETE FROM rag_documents WHERE metadata->>'source' = 'quarkus-rest'");
        assertThat(sql).contains("INSERT INTO rag_documents");
        assertThat(sql).contains("[0.1,0.2,0.3]");
        assertThat(sql).contains("Hello world");
        assertThat(sql).contains("quarkus-rest");
    }

    @Test
    void escapesSingleQuotesInSql() {
        assertThat(SqlFragmentWriter.escapeSql("it's a test")).isEqualTo("it''s a test");
    }

    @Test
    void escapesJsonSpecialCharacters() {
        assertThat(SqlFragmentWriter.escapeJson("line1\nline2")).isEqualTo("line1\\nline2");
        assertThat(SqlFragmentWriter.escapeJson("say \"hello\"")).isEqualTo("say \\\"hello\\\"");
    }

    @Test
    void formatsVectorCorrectly() {
        float[] vector = { 1.5f, -0.3f, 0.0f };
        String formatted = SqlFragmentWriter.formatVector(vector);
        assertThat(formatted).isEqualTo("[1.5,-0.3,0.0]");
    }

    @Test
    void formatsMetadataAsJson() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", "value with \"quotes\"");

        String json = SqlFragmentWriter.formatMetadata(metadata);
        assertThat(json).isEqualTo("{\"key1\":\"value1\",\"key2\":\"value with \\\"quotes\\\"\"}");
    }

    @Test
    void deleteStatementUsesSourceName(@TempDir Path tempDir) throws IOException {
        SqlFragmentWriter writer = new SqlFragmentWriter();
        Path output = tempDir.resolve("test.sql");

        writer.write(output, "my-extension", "1.0.0", List.of());

        String sql = Files.readString(output);
        assertThat(sql).contains("DELETE FROM rag_documents WHERE metadata->>'source' = 'my-extension'");
    }
}
