package io.quarkus.documentation.rag;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.DescriptionList;
import org.asciidoctor.ast.DescriptionListEntry;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.ListItem;
import org.asciidoctor.ast.Section;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.ast.Table;

/**
 * Processes AsciiDoc files into structured sections using AsciidoctorJ's AST.
 * Uses raw source text (getSource/getLines) rather than getContent() to avoid
 * triggering JRuby HTML rendering, which is slow and can OOM on large docs.
 */
public class AsciiDocProcessor implements AutoCloseable {

    private final Asciidoctor asciidoctor;

    public AsciiDocProcessor() {
        this.asciidoctor = Asciidoctor.Factory.create();
    }

    public ParsedDocument parse(Path adocPath) {
        Document document = asciidoctor.loadFile(adocPath.toFile(),
                Options.builder()
                        .sourcemap(true)
                        .build());

        String title = document.getDoctitle();
        List<DocumentSection> sections = new ArrayList<>();
        extractSections(document, sections, new ArrayList<>());

        return new ParsedDocument(title, sections);
    }

    private void extractSections(StructuralNode node, List<DocumentSection> sections, List<String> parentPath) {
        for (ContentNode child : node.getBlocks()) {
            if (child instanceof Section section) {
                List<String> currentPath = new ArrayList<>(parentPath);
                currentPath.add(section.getTitle());

                String content = renderSectionContent(section);
                if (!content.isBlank()) {
                    sections.add(new DocumentSection(
                            section.getLevel(),
                            section.getTitle(),
                            content,
                            String.join(" > ", currentPath)));
                }

                extractSections(section, sections, currentPath);
            }
        }
    }

    private String renderSectionContent(Section section) {
        StringBuilder sb = new StringBuilder();

        for (ContentNode block : section.getBlocks()) {
            if (block instanceof Section) {
                continue;
            }
            if (block instanceof StructuralNode structuralBlock) {
                appendBlockContent(structuralBlock, sb);
            }
        }

        return sb.toString().trim();
    }

    private void appendBlockContent(StructuralNode block, StringBuilder sb) {
        if (block instanceof Block b) {
            appendBlock(b, sb);
        } else if (block instanceof org.asciidoctor.ast.List list) {
            appendList(list, sb);
        } else if (block instanceof DescriptionList dlist) {
            appendDescriptionList(dlist, sb);
        } else if (block instanceof Table table) {
            appendTable(table, sb);
        } else {
            appendChildBlocks(block, sb);
        }
    }

    private void appendBlock(Block block, StringBuilder sb) {
        String context = block.getContext();

        switch (context) {
            case "listing", "literal" -> {
                String source = block.getSource();
                if (source != null && !source.isBlank()) {
                    sb.append("```\n").append(source).append("\n```\n\n");
                }
            }
            case "admonition" -> {
                String style = block.getStyle();
                String prefix = style != null ? style.toUpperCase() + ": " : "";
                String source = block.getSource();
                if (source != null && !source.isBlank()) {
                    sb.append(prefix).append(source).append("\n\n");
                } else {
                    appendChildBlocks(block, sb);
                }
            }
            default -> {
                String source = block.getSource();
                if (source != null && !source.isBlank()) {
                    sb.append(source).append("\n\n");
                } else {
                    appendChildBlocks(block, sb);
                }
            }
        }
    }

    private void appendList(org.asciidoctor.ast.List list, StringBuilder sb) {
        for (StructuralNode item : list.getItems()) {
            if (item instanceof ListItem li) {
                String text = li.getText();
                if (text != null && !text.isBlank()) {
                    sb.append("- ").append(stripHtml(text)).append("\n");
                }
                if (li.getBlocks() != null && !li.getBlocks().isEmpty()) {
                    for (ContentNode child : li.getBlocks()) {
                        if (child instanceof StructuralNode sn) {
                            appendBlockContent(sn, sb);
                        }
                    }
                }
            }
        }
        sb.append("\n");
    }

    private void appendDescriptionList(DescriptionList dlist, StringBuilder sb) {
        for (DescriptionListEntry entry : dlist.getItems()) {
            for (ListItem term : entry.getTerms()) {
                String text = term.getText();
                if (text != null) {
                    sb.append(stripHtml(text)).append(": ");
                }
            }
            ListItem desc = entry.getDescription();
            if (desc != null) {
                String text = desc.getText();
                if (text != null) {
                    sb.append(stripHtml(text));
                }
            }
            sb.append("\n");
        }
        sb.append("\n");
    }

    private void appendTable(Table table, StringBuilder sb) {
        for (var row : table.getHeader()) {
            appendTableRow(row, sb);
        }
        for (var row : table.getBody()) {
            appendTableRow(row, sb);
        }
        sb.append("\n");
    }

    private void appendTableRow(org.asciidoctor.ast.Row row, StringBuilder sb) {
        for (var cell : row.getCells()) {
            String text = cell.getText();
            if (text != null && !text.isBlank()) {
                sb.append(stripHtml(text)).append(" | ");
            }
        }
        sb.append("\n");
    }

    private void appendChildBlocks(StructuralNode block, StringBuilder sb) {
        if (block.getBlocks() != null) {
            for (ContentNode nested : block.getBlocks()) {
                if (nested instanceof StructuralNode nestedBlock) {
                    appendBlockContent(nestedBlock, sb);
                }
            }
        }
    }

    static String stripHtml(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll("<[^>]+>", "")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    @Override
    public void close() {
        asciidoctor.close();
    }

    public record ParsedDocument(String title, List<DocumentSection> sections) {
    }

    public record DocumentSection(int level, String title, String content, String headerPath) {
    }
}
