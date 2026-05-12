package io.quarkus.documentation.rag;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregates multiple quarkus-rag.sql fragment files into a single SQL file
 * with the schema preamble included.
 */
public class SqlAggregator {

    public void aggregate(Path searchRoot, Path outputPath) throws IOException {
        List<Path> fragments = findFragments(searchRoot);
        aggregate(fragments, outputPath);
    }

    public void aggregate(List<Path> fragments, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write(RagSchema.SCHEMA_PREAMBLE);
            writer.newLine();

            for (Path fragment : fragments) {
                writer.newLine();
                writer.write("-- Source: " + fragment.getFileName());
                writer.newLine();
                String content = Files.readString(fragment, StandardCharsets.UTF_8);
                writer.write(content);
                writer.newLine();
            }
        }
    }

    public List<Path> findFragments(Path searchRoot) throws IOException {
        List<Path> fragments = new ArrayList<>();

        Files.walkFileTree(searchRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(RagSchema.META_INF_PATH)
                        || file.getFileName().toString().equals("quarkus-rag.sql")) {
                    fragments.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        fragments.sort(Path::compareTo);
        return fragments;
    }
}
