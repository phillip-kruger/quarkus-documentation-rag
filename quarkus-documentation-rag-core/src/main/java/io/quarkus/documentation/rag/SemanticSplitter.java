package io.quarkus.documentation.rag;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.documentation.rag.AsciiDocProcessor.DocumentSection;

/**
 * Splits AsciiDoc sections into chunks suitable for embedding.
 * Adapted from the Markdown-based MarkdownSemanticSplitter but works
 * directly with the AsciiDoc AST sections from {@link AsciiDocProcessor}.
 */
public class SemanticSplitter {

    private static final int MIN_SECTION_SIZE = 300;

    private final int maxChunkSize;

    public SemanticSplitter(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }

    public List<Chunk> split(List<DocumentSection> sections) {
        if (sections.isEmpty()) {
            return List.of();
        }

        List<DocumentSection> merged = mergeSmallSections(sections);
        List<Chunk> chunks = new ArrayList<>();

        for (DocumentSection section : merged) {
            if (section.content().length() <= maxChunkSize) {
                chunks.add(new Chunk(
                        section.content(),
                        section.title(),
                        section.level(),
                        section.headerPath(),
                        null));
            } else {
                List<String> parts = splitLargeText(section.content());
                for (int i = 0; i < parts.size(); i++) {
                    chunks.add(new Chunk(
                            parts.get(i),
                            section.title(),
                            section.level(),
                            section.headerPath(),
                            (i + 1) + "/" + parts.size()));
                }
            }
        }

        return chunks;
    }

    private List<DocumentSection> mergeSmallSections(List<DocumentSection> sections) {
        List<DocumentSection> merged = new ArrayList<>();
        DocumentSection pending = null;

        for (DocumentSection current : sections) {
            if (pending == null) {
                pending = current;
                continue;
            }

            boolean shouldMerge = pending.content().length() < MIN_SECTION_SIZE;

            if (shouldMerge && (pending.level() <= 2 || current.level() <= 2)) {
                if (pending.content().length() >= 200) {
                    shouldMerge = false;
                }
            }

            if (shouldMerge && (pending.content().length() + current.content().length() > maxChunkSize)) {
                shouldMerge = false;
            }

            if (shouldMerge) {
                pending = mergeTwoSections(pending, current);
            } else {
                merged.add(pending);
                pending = current;
            }
        }

        if (pending != null) {
            merged.add(pending);
        }

        return merged;
    }

    private DocumentSection mergeTwoSections(DocumentSection first, DocumentSection second) {
        return new DocumentSection(
                first.level(),
                first.title() + " + " + second.title(),
                first.content() + "\n\n" + second.content(),
                first.headerPath() + " | " + second.headerPath());
    }

    private List<String> splitLargeText(String text) {
        List<String> parts = new ArrayList<>();
        String[] paragraphs = text.split("\n\n+");
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            if (current.length() + paragraph.length() + 2 > maxChunkSize && !current.isEmpty()) {
                parts.add(current.toString().trim());
                current = new StringBuilder();
            }
            if (!current.isEmpty()) {
                current.append("\n\n");
            }
            current.append(paragraph);
        }

        if (!current.isEmpty()) {
            parts.add(current.toString().trim());
        }

        return parts;
    }

    public record Chunk(String text, String sectionTitle, int sectionLevel, String sectionPath, String sectionPart) {
    }
}
