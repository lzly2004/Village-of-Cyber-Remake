package com.vocr.ui;

import org.junit.jupiter.api.Test;

import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class UIHelpersTest
{
    @Test
    void scaleIcon_shouldReuseOriginalWhenDimensionsAlreadyMatch()
    {
        ImageIcon original = new ImageIcon(new BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB));

        assertSame(original, UIHelpers.scaleIcon(original, 20, 10));
        assertNull(UIHelpers.scaleIcon(null, 20, 10));
    }

    @Test
    void scaleIcon_shouldCreateRequestedSizeWhenDimensionsDiffer()
    {
        ImageIcon original = new ImageIcon(new BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB));

        ImageIcon scaled = UIHelpers.scaleIcon(original, 10, 5);

        assertNotSame(original, scaled);
        assertEquals(10, scaled.getIconWidth());
        assertEquals(5, scaled.getIconHeight());
    }
}
