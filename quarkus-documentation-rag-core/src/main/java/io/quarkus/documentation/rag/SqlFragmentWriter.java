package io.quarkus.documentation.rag;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Writes RAG chunks with embeddings as SQL INSERT statements.
 * Each fragment is idempotent: it deletes its own previous data before inserting.
 */
public class SqlFragmentWriter {

    public void write(Path outputPath, String extensionName, String version,
            List<ChunkWithEmbedding> chunks) throws IOException {
        Files.createDirectories(outputPath.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writeHeader(writer, extensionName, version);
            writeDelete(writer, extensionName);

            for (ChunkWithEmbedding chunk : chunks) {
                writeInsert(writer, chunk);
            }
        }
    }

    private void writeHeader(BufferedWriter writer, String extensionName, String version) throws IOException {
        writer.write("-- quarkus-rag fragment: ");
        writer.write(extensionName);
        writer.write(" ");
        writer.write(version);
        writer.newLine();
        writer.write("-- Generated: ");
        writer.write(Instant.now().toString());
        writer.newLine();
        writer.newLine();
    }

    private void writeDelete(BufferedWriter writer, String extensionName) throws IOException {
        writer.write("DELETE FROM ");
        writer.write(RagSchema.TABLE_NAME);
        writer.write(" WHERE metadata->>'source' = '");
        writer.write(escapeSql(extensionName));
        writer.write("';");
        writer.newLine();
        writer.newLine();
    }

    private void writeInsert(BufferedWriter writer, ChunkWithEmbedding chunk) throws IOException {
        String uuid = UUID.randomUUID().toString();
        String vectorLiteral = formatVector(chunk.embedding());
        String metadataJson = formatMetadata(chunk.metadata());

        writer.write("INSERT INTO ");
        writer.write(RagSchema.TABLE_NAME);
        writer.write(" (embedding_id, embedding, text, metadata) VALUES ('");
        writer.write(uuid);
        writer.write("', '");
        writer.write(vectorLiteral);
        writer.write("'::vector, '");
        writer.write(escapeSql(chunk.text()));
        writer.write("', '");
        writer.write(escapeSql(metadataJson));
        writer.write("'::jsonb);");
        writer.newLine();
    }

    static String formatVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    static String formatMetadata(Map<String, String> metadata) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(escapeJson(entry.getKey())).append('"');
            sb.append(':');
            sb.append('"').append(escapeJson(entry.getValue())).append('"');
        }
        sb.append('}');
        return sb.toString();
    }

    static String escapeSql(String value) {
        return value.replace("'", "''");
    }

    static String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static Map<String, String> buildMetadata(String source, String version,
            String title, String url, Map<String, String> adocMetadata, SemanticSplitter.Chunk chunk) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", source);
        metadata.put("version", version);
        if (title != null) {
            metadata.put("title", title);
        }
        if (url != null) {
            metadata.put("url", url);
        }
        if (adocMetadata.containsKey("topics")) {
            metadata.put("topics", adocMetadata.get("topics"));
        }
        if (adocMetadata.containsKey("categories")) {
            metadata.put("categories", adocMetadata.get("categories"));
        }
        if (adocMetadata.containsKey("extensions")) {
            metadata.put("extensions", adocMetadata.get("extensions"));
        }
        if (adocMetadata.containsKey("summary")) {
            metadata.put("summary", adocMetadata.get("summary"));
        }
        metadata.put("section_title", chunk.sectionTitle());
        metadata.put("section_level", String.valueOf(chunk.sectionLevel()));
        metadata.put("section_path", chunk.sectionPath());
        if (chunk.sectionPart() != null) {
            metadata.put("section_part", chunk.sectionPart());
        }
        return metadata;
    }

    public record ChunkWithEmbedding(String text, float[] embedding, Map<String, String> metadata) {
    }
}
