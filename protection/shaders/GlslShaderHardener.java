package protection.shaders;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.regex.Pattern;

/**
 * GLSL Safe Minification & Hardening Pipeline (Phase 8).
 * Strips developer comments, compresses redundant whitespace, but strictly
 * preserves directives (#version, #moj_import, #define, #extension) on independent lines
 * and retains uniform/sampler/attribute identifiers untouched.
 */
public final class GlslShaderHardener {

    private static final Pattern MULTI_LINE_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    private GlslShaderHardener() {}

    public static String minifyGlsl(String source) {
        if (source == null) return "";

        // 1. Remove multi-line block comments
        String text = MULTI_LINE_COMMENT.matcher(source).replaceAll("");

        // 2. Line-by-line processing
        String[] rawLines = text.split("\\r?\\n");
        StringBuilder output = new StringBuilder();

        for (String line : rawLines) {
            String trimmed = line.trim();
            // Remove single line comments
            int commentIdx = trimmed.indexOf("//");
            if (commentIdx != -1) {
                trimmed = trimmed.substring(0, commentIdx).trim();
            }

            if (trimmed.isEmpty()) {
                continue;
            }

            // Ensure preprocessor directives have their own dedicated line with newline
            if (trimmed.startsWith("#")) {
                output.append(trimmed).append("\n");
            } else {
                // Compress multiple whitespace sequences into single spaces
                String normalized = trimmed.replaceAll("\\s+", " ");
                output.append(normalized).append("\n");
            }
        }

        return output.toString();
    }

    public static int processDirectory(Path sourceDir, Path targetDir) throws IOException {
        int count = 0;
        if (!Files.exists(sourceDir)) {
            return 0;
        }

        try (var stream = Files.walk(sourceDir)) {
            List<Path> files = stream.filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.endsWith(".fsh") || name.endsWith(".vsh") || name.endsWith(".glsl");
                    }).toList();

            for (Path file : files) {
                Path relative = sourceDir.relativize(file);
                Path dest = targetDir.resolve(relative);
                Files.createDirectories(dest.getParent());

                String original = Files.readString(file, StandardCharsets.UTF_8);
                String minified = minifyGlsl(original);
                Files.writeString(dest, minified, StandardCharsets.UTF_8);
                count++;
            }
        }
        return count;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: GlslShaderHardener <sourceDir> <targetDir>");
            System.exit(1);
        }
        Path src = Path.of(args[0]);
        Path dest = Path.of(args[1]);
        int processed = processDirectory(src, dest);
        System.out.println("[GLSL Hardener] Processed " + processed + " shaders into " + dest);
    }
}
