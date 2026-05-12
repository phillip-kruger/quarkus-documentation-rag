package io.quarkus.documentation.rag;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AsciiDocMetadataExtractor {

    private static final Pattern ATTR_PATTERN =
            Pattern.compile("^\\s*:(categories|summary|extensions|topics):\\s*(.*)\\s*$");

    private static final int MAX_SCAN_LINES = 120;
    private static final int KNOWN_ATTRIBUTE_COUNT = 4;

    public static Map<String, String> extractMetadata(Path adocPath) {
        Map<String, String> metadata = new HashMap<>();

        if (!Files.isRegularFile(adocPath)) {
            return metadata;
        }

        try (BufferedReader br = Files.newBufferedReader(adocPath, StandardCharsets.UTF_8)) {
            String line;
            int lineCount = 0;

            while ((line = br.readLine()) != null) {
                if (++lineCount > MAX_SCAN_LINES) {
                    break;
                }

                Matcher m = ATTR_PATTERN.matcher(line);
                if (m.matches()) {
                    metadata.put(m.group(1).toLowerCase(), m.group(2).trim());

                    if (metadata.size() == KNOWN_ATTRIBUTE_COUNT) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            // Return whatever we have
        }

        return metadata;
    }
}
