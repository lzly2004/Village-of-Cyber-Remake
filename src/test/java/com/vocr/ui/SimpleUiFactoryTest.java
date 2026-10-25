package com.vocr.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.Font;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleUiFactoryTest
{
    @Test
    void textAreaEntries_shouldPreserveTheirThreeStyles()
    {
        JTextArea basic = TextareaSimpleFactory.createBasicTextArea(Color.CYAN);
        assertTextDefaults(basic);
        assertEquals(Font.PLAIN, basic.getFont().getStyle());
        assertEquals(26, basic.getFont().getSize());
        assertEquals(Color.CYAN, basic.getForeground());
        assertFalse(basic.isOpaque());

        JTextArea title = TextareaSimpleFactory.createBoldTitleTextArea(
                Color.BLACK, 20, "title");
        assertTextDefaults(title);
        assertEquals(Font.BOLD, title.getFont().getStyle());
        assertEquals(20, title.getFont().getSize());
        assertEquals(Color.BLACK, title.getForeground());
        assertEquals("title", title.getText());
        assertFalse(title.isOpaque());

        JTextArea tip = TextareaSimpleFactory.createTranslucentTipTextArea("tip");
        assertTextDefaults(tip);
        assertEquals(Font.BOLD, tip.getFont().getStyle());
        assertEquals(24, tip.getFont().getSize());
        assertEquals(Color.WHITE, tip.getForeground());
        assertEquals(UIConstants.COLOR_TRANSLUCENT_BLACK, tip.getBackground());
        assertEquals("tip", tip.getText());
        assertTrue(tip.isOpaque());
    }

    @Test
    void panelEntries_shouldPreserveSizeBoundsAndTransparentBackgroundOption()
    {
        JPanel sized = PanelSimpleFactory.createSimplePanel(100, 80, true, true);
        assertNull(sized.getLayout());
        assertEquals(100, sized.getWidth());
        assertEquals(80, sized.getHeight());
        assertTrue(sized.isOpaque());
        assertEquals(UIConstants.COLOR_TRANSPARENT, sized.getBackground());

        JPanel bounded = PanelSimpleFactory.createSimplePanel(10, 20, 100, 80, false, false);
        assertEquals(10, bounded.getX());
        assertEquals(20, bounded.getY());
        assertEquals(100, bounded.getWidth());
        assertEquals(80, bounded.getHeight());
        assertFalse(bounded.isOpaque());
    }

    private static void assertTextDefaults(JTextArea textArea)
    {
        assertFalse(textArea.isEditable());
        assertTrue(textArea.getLineWrap());
        assertTrue(textArea.getWrapStyleWord());
        assertFalse(textArea.isFocusable());
        assertNull(textArea.getHighlighter());
    }
}
