package com.vocr.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 防止界面或业务代码重新直接依赖图片、音频的实体文件名。 */
class AssetFilenameBoundaryTest {
    private static final Path PRODUCTION_SOURCE = Path.of("src/main/java/com/vocr");
    private static final Pattern ASSET_LITERAL =
            Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\\.(?:png|wav)\"");
    private static final Set<String> CATALOG_SOURCES = Set.of(
            "resources/UiImage.java",
            "resources/ImageNames.java",
            "resources/SoundEffect.java",
            "resources/BgmTrack.java");

    @Test
    void assetFileNamesShouldRemainInsideResourceCatalogs() throws Exception {
        List<String> violations = new ArrayList<>();
        try (var sources = Files.walk(PRODUCTION_SOURCE)) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                String relativePath = PRODUCTION_SOURCE.relativize(source)
                        .toString().replace('\\', '/');
                if (CATALOG_SOURCES.contains(relativePath)) {
                    continue;
                }

                List<String> lines = Files.readAllLines(source);
                for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
                    if (ASSET_LITERAL.matcher(lines.get(lineNumber)).find()) {
                        violations.add(relativePath + ":" + (lineNumber + 1)
                                + " -> " + lines.get(lineNumber).trim());
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(),
                () -> "图片和音频文件名只能定义在资源目录类中：\n" + String.join("\n", violations));
    }
}
