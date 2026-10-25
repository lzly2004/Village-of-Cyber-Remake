package com.vocr.resources;

import org.junit.jupiter.api.Test;

import javax.swing.ImageIcon;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiImageTest {

    @Test
    void everyFixedImageShouldExistOnClasspath() {
        for (UiImage image : UiImage.values()) {
            assertNotNull(
                    UiImageTest.class.getResource(image.resourcePath()),
                    () -> "缺少固定图片资源: " + image.resourcePath());
        }
    }

    @Test
    void everyFixedImageShouldMatchExactPackagedFileName() throws IOException {
        Set<String> actualFileNames;
        try (var files = Files.list(Path.of("src/main/resources/images"))) {
            actualFileNames = files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toSet());
        }

        for (UiImage image : UiImage.values()) {
            String fileName = image.resourcePath().substring("/images/".length());
            assertTrue(actualFileNames.contains(fileName),
                    () -> "固定图片不存在或文件名大小写不一致: " + fileName);
        }
    }

    @Test
    void resourcesShouldCacheImagesAndReuseFallbackForMissingFiles() {
        Resources resources = new Resources();

        ImageIcon startButton = resources.getImage(UiImage.START_BUTTON);
        assertSame(startButton, resources.getImage(UiImage.START_BUTTON));
        assertTrue(startButton.getIconWidth() > 0, "开始按钮图片应能正常解码");

        ImageIcon fallback = resources.getImage(UiImage.FALLBACK);
        assertSame(fallback, resources.getImage("__missing-fixed-image__.png"));
        assertTrue(fallback.getIconWidth() > 0, "兜底图片应能正常解码");
    }
}
