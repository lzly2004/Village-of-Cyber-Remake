package com.vocr.ui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.List;

/** Same read-only information board, with per-line color instead of one global foreground. */
final class ReplayGuardTextPane {
    static final Color UNANNOUNCED = new Color(0x666666);

    static JTextPane create(String text, List<ReplayGuardHistory.Line> guards) {
        var pane = new JTextPane();
        pane.setEditable(false); pane.setOpaque(false); pane.setFocusable(false);
        pane.setBorder(BorderFactory.createEmptyBorder()); pane.setMargin(new Insets(0, 0, 0, 0));
        pane.setHighlighter(null);
        pane.setFont(new Font(UIConstants.FONT_FAMILY, Font.BOLD, 24));
        pane.setForeground(Color.BLACK); pane.setBackground(UIConstants.COLOR_TRANSPARENT);
        pane.setText(text);
        var document = pane.getStyledDocument();
        var normal = new SimpleAttributeSet();
        StyleConstants.setForeground(normal, Color.BLACK);
        StyleConstants.setFontFamily(normal, UIConstants.FONT_FAMILY);
        StyleConstants.setFontSize(normal, 24); StyleConstants.setBold(normal, true);
        document.setCharacterAttributes(0, text.length(), normal, false);
        int position = text.lastIndexOf(GameStrings.SECTION_GUARD) + GameStrings.SECTION_GUARD.length();
        for (var guard : guards) {
            if (guard.unannounced()) {
                var muted = new SimpleAttributeSet(); StyleConstants.setForeground(muted, UNANNOUNCED);
                document.setCharacterAttributes(position, guard.text().length(), muted, false);
            }
            position += guard.text().length();
        }
        pane.setCaretPosition(0);
        pane.getAccessibleContext().setAccessibleDescription("淡色の護衛行は、この再生日には狩人COしていません。");
        return pane;
    }
}
