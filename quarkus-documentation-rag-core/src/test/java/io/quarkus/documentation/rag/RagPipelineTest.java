package io.quarkus.documentation.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RagPipelineTest {

    private static final Path TEST_GUIDE = Path.of("src/test/resources/test-guide.adoc");
    private static final Path GUIDES_DIR = Path.of("src/test/resources/guides");

    @Test
    void extensionModeProducesValidSql(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("rag.sql");

        try (RagPipeline pipeline = new RagPipeline(
                "quarkus-rest", "3.21.0", "https://quarkus.io/guides", null, 1000)) {
            pipeline.processGuides(List.of(TEST_GUIDE), output);
        }

        String sql = Files.readString(output);

        assertThat(sql).contains("-- quarkus-rag fragment: quarkus-rest 3.21.0");
        assertThat(sql).contains("DELETE FROM rag_documents WHERE metadata->>'source' = 'quarkus-rest'");
        assertThat(sql).contains("INSERT INTO rag_documents");
        assertThat(sql).contains("::vector");
        assertThat(sql).contains("::jsonb");

        long insertCount = sql.lines()
                .filter(line -> line.startsWith("INSERT INTO"))
                .count();
        assertThat(insertCount).isGreaterThan(0);
    }

    @Test
    void extensionModeTagsAllChunksWithFixedSource() {
        try (RagPipeline pipeline = new RagPipeline(
                "quarkus-rest", "3.21.0", "https://quarkus.io/guides", null, 1000)) {
            var chunks = pipeline.processGuide(TEST_GUIDE);

            assertThat(chunks).isNotEmpty();
            for (var chunk : chunks) {
                assertThat(chunk.text()).isNotBlank();
                assertThat(chunk.embedding()).hasSize(RagSchema.EMBEDDING_DIMENSION);
                assertThat(chunk.metadata()).containsEntry("source", "quarkus-rest");
            }
        }
    }

    @Test
    void directoryModeProcessesAllGuidesExcludingUnderscoreFiles(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("rag.sql");

        try (RagPipeline pipeline = new RagPipeline(
                null, "3.21.0", "https://quarkus.io/guides", null, 1000)) {
            pipeline.processDirectory(GUIDES_DIR, output);
        }

        String sql = Files.readString(output);

        assertThat(sql).contains("INSERT INTO rag_documents");
        // Should not contain _attributes.adoc content
        assertThat(sql).doesNotContain("_attributes");
    }

    @Test
    void directoryModeDerivesSourceFromExtensionsMetadata() {
        try (RagPipeline pipeline = new RagPipeline(
                null, "3.21.0", "https://quarkus.io/guides", null, 1000)) {
            // rest.adoc has :extensions: io.quarkus:quarkus-rest,io.quarkus:quarkus-rest-jackson
            var chunks = pipeline.processGuide(GUIDES_DIR.resolve("rest.adoc"));

            assertThat(chunks).isNotEmpty();
            assertThat(chunks.get(0).metadata()).containsEntry("source", "quarkus-rest");
        }
    }

    @Test
    void directoryModeFallsToGuideNameWhenNoExtensions() {
        try (RagPipeline pipeline = new RagPipeline(
                null, "3.21.0", "https://quarkus.io/guides", null, 1000)) {
            // getting-started.adoc has no :extensions: attribute
            var chunks = pipeline.processGuide(GUIDES_DIR.resolve("getting-started.adoc"));

            assertThat(chunks).isNotEmpty();
            assertThat(chunks.get(0).metadata()).containsEntry("source", "quarkus-getting-started");
        }
    }

    @Test
    void resolveSourceNameUsesFixedNameInExtensionMode() {
        try (RagPipeline pipeline = new RagPipeline(
                "my-extension", "1.0.0", null, null, 1000)) {
            String source = pipeline.resolveSourceName("some-guide", Map.of());
            assertThat(source).isEqualTo("my-extension");
        }
    }

    @Test
    void resolveSourceNameExtractsArtifactIdFromExtensionsMetadata() {
        try (RagPipeline pipeline = new RagPipeline(
                null, "1.0.0", null, null, 1000)) {
            String source = pipeline.resolveSourceName("rest",
                    Map.of("extensions", "io.quarkus:quarkus-rest,io.quarkus:quarkus-rest-jackson"));
            assertThat(source).isEqualTo("quarkus-rest");
        }
    }

    @Test
    void resolveSourceNameHandlesSimpleExtensionNames() {
        try (RagPipeline pipeline = new RagPipeline(
                null, "1.0.0", null, null, 1000)) {
            String source = pipeline.resolveSourceName("rest",
                    Map.of("extensions", "quarkus-rest"));
            assertThat(source).isEqualTo("quarkus-rest");
        }
    }

    @Test
    void resolveSourceNameFallsBackToGuideFilename() {
        try (RagPipeline pipeline = new RagPipeline(
                null, "1.0.0", null, null, 1000)) {
            String source = pipeline.resolveSourceName("getting-started", Map.of());
            assertThat(source).isEqualTo("quarkus-getting-started");
        }
    }

    @Test
    void guideUrlOverridesConstructedUrl() {
        String customUrl = "https://docs.quarkiverse.io/quarkus-vault/dev/index.html";
        try (RagPipeline pipeline = new RagPipeline(
                "quarkus-vault", "1.2.0", "https://quarkus.io/guides", customUrl, 1000)) {
            var chunks = pipeline.processGuide(TEST_GUIDE);

            assertThat(chunks).isNotEmpty();
            assertThat(chunks.get(0).metadata()).containsEntry("url", customUrl);
            // Should NOT contain the constructed quarkus.io URL
            assertThat(chunks.get(0).metadata().get("url")).doesNotContain("quarkus.io/guides");
        }
    }

    @Test
    void metadataUsesVersionKeyNotQuarkusVersion() {
        try (RagPipeline pipeline = new RagPipeline(
                "quarkus-rest", "3.21.0", "https://quarkus.io/guides", null, 1000)) {
            var chunks = pipeline.processGuide(TEST_GUIDE);

            assertThat(chunks).isNotEmpty();
            assertThat(chunks.get(0).metadata()).containsEntry("version", "3.21.0");
            assertThat(chunks.get(0).metadata()).doesNotContainKey("quarkus_version");
        }
    }

    @Test
    void findGuidesExcludesUnderscoreFiles() throws IOException {
        List<Path> guides = RagPipeline.findGuides(GUIDES_DIR);

        assertThat(guides)
                .extracting(p -> p.getFileName().toString())
                .contains("rest.adoc", "getting-started.adoc")
                .doesNotContain("_attributes.adoc");
    }
}
