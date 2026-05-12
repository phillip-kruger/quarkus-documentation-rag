package io.quarkus.documentation.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EmbeddingGeneratorTest {

    private static EmbeddingGenerator generator;

    @BeforeAll
    static void setUp() {
        generator = new EmbeddingGenerator();
    }

    @AfterAll
    static void tearDown() {
        generator.close();
    }

    @Test
    void generates384DimensionEmbedding() {
        float[] embedding = generator.embed("Hello world");
        assertThat(embedding).hasSize(RagSchema.EMBEDDING_DIMENSION);
    }

    @Test
    void differentTextsProduceDifferentEmbeddings() {
        float[] e1 = generator.embed("REST endpoints in Quarkus");
        float[] e2 = generator.embed("Database migrations with Flyway");

        assertThat(e1).isNotEqualTo(e2);
    }

    @Test
    void embedsMultipleTextsInBatch() {
        List<float[]> embeddings = generator.embedAll(List.of(
                "First text about REST",
                "Second text about GraphQL"));

        assertThat(embeddings).hasSize(2);
        assertThat(embeddings.get(0)).hasSize(RagSchema.EMBEDDING_DIMENSION);
        assertThat(embeddings.get(1)).hasSize(RagSchema.EMBEDDING_DIMENSION);
    }
}
