package com.vocr.resources;

import org.junit.jupiter.api.Test;

import javax.swing.ImageIcon;

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
