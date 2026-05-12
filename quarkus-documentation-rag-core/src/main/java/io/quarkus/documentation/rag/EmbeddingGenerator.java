package io.quarkus.documentation.rag;

import java.util.List;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;

/**
 * Generates vector embeddings using the BGE Small EN v1.5 quantized model (384 dimensions).
 * This is an in-process ONNX model — no external service required.
 */
public class EmbeddingGenerator implements AutoCloseable {

    private final EmbeddingModel model;

    public EmbeddingGenerator() {
        this.model = new BgeSmallEnV15QuantizedEmbeddingModel();
    }

    public float[] embed(String text) {
        Embedding embedding = model.embed(text).content();
        return embedding.vector();
    }

    public List<float[]> embedAll(List<String> texts) {
        return model.embedAll(texts.stream()
                .map(dev.langchain4j.data.segment.TextSegment::from)
                .toList())
                .content()
                .stream()
                .map(Embedding::vector)
                .toList();
    }

    @Override
    public void close() {
        // ONNX model cleanup if needed
    }
}
