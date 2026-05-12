package io.quarkus.documentation.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AsciiDocMetadataExtractorTest {

    private static final Path TEST_GUIDE = Path.of("src/test/resources/test-guide.adoc");

    @Test
    void extractsAllMetadataAttributes() {
        Map<String, String> metadata = AsciiDocMetadataExtractor.extractMetadata(TEST_GUIDE);

        assertThat(metadata).containsEntry("categories", "web");
        assertThat(metadata).containsEntry("topics", "rest, http, json");
        assertThat(metadata).containsEntry("extensions", "io.quarkus:quarkus-rest");
        assertThat(metadata).containsEntry("summary", "This guide explains how to build REST endpoints with Quarkus.");
    }

    @Test
    void returnsEmptyMapForNonexistentFile() {
        Map<String, String> metadata = AsciiDocMetadataExtractor.extractMetadata(Path.of("nonexistent.adoc"));
        assertThat(metadata).isEmpty();
    }
}
