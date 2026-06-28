import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * UI组件统一工厂：合并 Button/Label/Panel/Textarea 四个 SimpleFactory
 * 所有方法签名保持不变，调用方只需改 import
 * @deprecated 建议逐步迁移至此类，原工厂类保留以确保兼容性
 */
@Deprecated
public class UIFactory
{
    // ==================== Button 部分 ====================
    public static JButton makeButton(int kind, ImageIcon icon) {
        return makeButton(kind, null, icon);
    }
    public static JButton makeButton(int kind, String text) {
        return makeButton(kind, text, null);
    }
    public static JButton makeButton(int kind, String text, ImageIcon icon) {
        switch (kind) {
            case ButtonConst.Simple_Button -> { return createSimpleButton(text, icon); }
            case ButtonConst.Draggable_Button -> { return createDraggableButton(text, icon); }
        }
        return null;
    }
    public static void setButton(JButton btn) {
        btn.setOpaque(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setForeground(Color.white);
        btn.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, GameConstants.FONT_SIZE_BUTTON));
        btn.setFocusPainted(false);
    }
    public static JButton createDraggableButton(String text, ImageIcon icon) {
        JButton draggableBtn = createSimpleButton(text, icon);
        addDragListeners(draggableBtn);
        return draggableBtn;
    }
    private static void addDragListeners(JButton draggableBtn) {
        draggableBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        draggableBtn.setPreferredSize(new Dimension(80, 30));
        final Point initPos = new Point();
        final int[] mouseOffset = new int[2];
        draggableBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                initPos.setLocation(draggableBtn.getX(), draggableBtn.getY());
                mouseOffset[0] = e.getX();
                mouseOffset[1] = e.getY();
                draggableBtn.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                Container parent = draggableBtn.getParent();
                if (parent != null) {
                    parent.setComponentZOrder(draggableBtn, 0);
                    parent.repaint();
                }
            }
        });
        draggableBtn.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Container parent = draggableBtn.getParent();
                if (parent == null || parent.getWidth() <= 0 || parent.getHeight() <= 0
                        || draggableBtn.getWidth() <= 0 || draggableBtn.getHeight() <= 0) { return; }
                Point screenPos = e.getLocationOnScreen();
                SwingUtilities.convertPointFromScreen(screenPos, parent);
                int newX = screenPos.x - mouseOffset[0];
                int newY = screenPos.y - mouseOffset[1];
                newX = Math.max(0, Math.min(newX, parent.getWidth() - draggableBtn.getWidth()));
                newY = Math.max(0, Math.min(newY, parent.getHeight() - draggableBtn.getHeight()));
                draggableBtn.setBounds(newX, newY, draggableBtn.getWidth(), draggableBtn.getHeight());
            }
        });
    }
    public static JButton createSimpleButton(String text, ImageIcon icon) {
        JButton btn = new JButton();
        if (icon != null) btn.setIcon(icon);
        if (text != null) btn.setText(text);
        setButton(btn);
        return btn;
    }
    public static JButton makeButton(int kind, int x, int y, int width, int height, ImageIcon icon) {
        return makeButton(kind, x, y, width, height, null, icon);
    }
    public static JButton makeButton(int kind, int x, int y, ImageIcon icon) {
        return makeButton(kind, x, y, icon.getIconWidth(), icon.getIconHeight(), null, icon);
    }
    public static JButton makeButton(int kind, int x, int y, int width, int height, String text, ImageIcon icon) {
        switch (kind) {
            case ButtonConst.Simple_Button -> { return createSimpleBoundedButton(x, y, width, height, text, icon); }
            case ButtonConst.Draggable_Button -> { return createDraggableBoundedButton(x, y, width, height, text, icon); }
        }
        return null;
    }
    public static JButton createDraggableBoundedButton(int x, int y, int width, int height, String text, ImageIcon icon) {
        JButton draggableBtn = createSimpleBoundedButton(x, y, width, height, text, icon);
        addDragListeners(draggableBtn);
        return draggableBtn;
    }
    public static JButton createSimpleBoundedButton(int x, int y, int width, int height, String text, ImageIcon icon) {
        JButton btn = new JButton();
        btn.setBounds(x, y, width, height);
        if (icon != null) {
            ImageIcon scaledIcon = ConstNum.scaleIcon(icon, width, height);
            btn.setIcon(scaledIcon);
        }
        if (text != null) btn.setText(text);
        setButton(btn);
        return btn;
    }

    // ==================== Label 部分 ====================
    public static JLabel makeLabel(int kind, String text, ImageIcon icon) {
        switch (kind) {
            case LabelConst.Simple_Label -> { return createSimpleLabel(text, icon); }
            case LabelConst.Text_Label -> { return createTextLabel(text, icon); }
            case LabelConst.Black_Label -> { return createBlackLabel(text, icon); }
        }
        return null;
    }
    public static JLabel makeLabel(int kind, ImageIcon icon) { return makeLabel(kind, null, icon); }
    public static JLabel makeLabel(int kind, String text) { return makeLabel(kind, text, null); }
    public static JLabel createSimpleLabel(String text, ImageIcon icon) {
        JLabel jLabel = new JLabel();
        if (icon != null) jLabel.setIcon(icon);
        if (text != null) jLabel.setText(text);
        jLabel.setOpaque(false);
        jLabel.setFocusable(false);
        return jLabel;
    }
    public static JLabel createTextLabel(String text, ImageIcon icon) {
        JLabel jLabel = new JLabel();
        if (icon != null) jLabel.setIcon(icon);
        if (text != null) jLabel.setText(text);
        jLabel.setForeground(Color.WHITE);
        jLabel.setFont(new Font(GameConstants.FONT_FAMILY, Font.PLAIN, GameConstants.FONT_SIZE_DIALOG));
        jLabel.setOpaque(false);
        return jLabel;
    }
    public static JLabel createBlackLabel(String text, ImageIcon icon) {
        JLabel jLabel = new JLabel();
        if (icon != null) jLabel.setIcon(icon);
        if (text != null) jLabel.setText(text);
        jLabel.setOpaque(false);
        jLabel.setForeground(Color.BLACK);
        jLabel.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, GameConstants.FONT_SIZE_TITLE));
        return jLabel;
    }
    public static JLabel makeLabel(int kind, int x, int y, int width, int height, ImageIcon icon) {
        return makeLabel(kind, x, y, width, height, null, icon);
    }
    public static JLabel makeLabel(int kind, int x, int y, int width, int height, String text) {
        return makeLabel(kind, x, y, width, height, text, null);
    }
    public static JLabel makeLabel(int kind, int x, int y, ImageIcon icon) {
        return makeLabel(kind, x, y, icon.getIconWidth(), icon.getIconHeight(), null, icon);
    }
    public static JLabel makeLabel(int kind, int x, int y, int width, int height, String text, ImageIcon icon) {
        switch (kind) {
            case LabelConst.Simple_Label -> { return createSimpleBoundedLabel(x, y, width, height, text, icon); }
            case LabelConst.Text_Label -> { return createTextBoundedLabel(x, y, width, height, text, icon); }
            case LabelConst.Black_Label -> { return createBlackBoundedLabel(x, y, width, height, text, icon); }
        }
        return null;
    }
    public static JLabel createSimpleBoundedLabel(int x, int y, int width, int height, String text, ImageIcon icon) {
        JLabel jLabel = new JLabel();
        jLabel.setBounds(x, y, width, height);
        if (icon != null) {
            ImageIcon scaledicon = ConstNum.scaleIcon(icon, width, height);
            jLabel.setIcon(scaledicon);
        }
        if (text != null) jLabel.setText(text);
        jLabel.setOpaque(false);
        jLabel.setFocusable(false);
        return jLabel;
    }
    public static JLabel createTextBoundedLabel(int x, int y, int width, int height, String text, ImageIcon icon) {
        JLabel jLabel = new JLabel();
        jLabel.setBounds(x, y, width, height);
        if (icon != null) {
            ImageIcon scaledicon = ConstNum.scaleIcon(icon, width, height);
            jLabel.setIcon(scaledicon);
        }
        if (text != null) jLabel.setText(text);
        jLabel.setForeground(Color.WHITE);
        jLabel.setFont(new Font(GameConstants.FONT_FAMILY, Font.PLAIN, GameConstants.FONT_SIZE_DIALOG));
        jLabel.setOpaque(false);
        return jLabel;
    }
    public static JLabel createBlackBoundedLabel(int x, int y, int width, int height, String text, ImageIcon icon) {
        JLabel jLabel = new JLabel();
        jLabel.setBounds(x, y, width, height);
        if (icon != null) {
            ImageIcon scaledicon = ConstNum.scaleIcon(icon, width, height);
            jLabel.setIcon(scaledicon);
        }
        if (text != null) jLabel.setText(text);
        jLabel.setOpaque(false);
        jLabel.setForeground(Color.BLACK);
        jLabel.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, GameConstants.FONT_SIZE_TITLE));
        return jLabel;
    }

    // ==================== Panel 部分 ====================
    public static JPanel makePanel(int kind, int width, int height, boolean isOpaque, boolean isWhiteBk) {
        switch (kind) {
            case PanelConst.Simple_Panel -> { return createSimplePanel(width, height, isOpaque, isWhiteBk); }
        }
        return null;
    }
    public static JPanel createSimplePanel(int width, int height, boolean isOpaque, boolean isWhiteBk) {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(null);
        if (width > 0 && height > 0) { jPanel.setSize(width, height); }
        if (isWhiteBk) jPanel.setBackground(GameConstants.COLOR_TRANSPARENT);
        jPanel.setOpaque(isOpaque);
        return jPanel;
    }
    public static JPanel makePanel(int kind, int x, int y, int width, int height, boolean isOpaque, boolean isWhiteBk) {
        switch (kind) {
            case PanelConst.Simple_Panel -> { return createSimplePanel(x, y, width, height, isOpaque, isWhiteBk); }
        }
        return null;
    }
    public static JPanel createSimplePanel(int x, int y, int width, int height, boolean isOpaque, boolean isWhiteBk) {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(null);
        if (width > 0 && height > 0) { jPanel.setBounds(x, y, width, height); }
        if (isWhiteBk) jPanel.setBackground(GameConstants.COLOR_TRANSPARENT);
        jPanel.setOpaque(isOpaque);
        return jPanel;
    }

    // ==================== Textarea 部分 ====================
    private static final boolean DEFAULT_EDITABLE = false;
    private static final boolean DEFAULT_LINE_WRAP = true;
    private static final boolean DEFAULT_WRAP_STYLE_WORD = true;
    private static final boolean DEFAULT_OPAQUE = false;
    private static final Border DEFAULT_BORDER = BorderFactory.createEmptyBorder();

    public static JTextArea createBasicTextArea(Color color) {
        return createTextAreaBuilder()
                .fontStyle(Font.PLAIN)
                .fontSize(26)
                .foreground(color)
                .build();
    }
    public static JTextArea createBoldTitleTextArea(Color color, int fontSize, String text) {
        return createTextAreaBuilder()
                .fontStyle(Font.BOLD)
                .fontSize(fontSize)
                .text(text)
                .foreground(color)
                .build();
    }
    public static JTextArea createTranslucentTipTextArea(String text) {
        return createTextAreaBuilder()
                .fontStyle(Font.BOLD)
                .fontSize(24)
                .text(text)
                .background(GameConstants.COLOR_TRANSLUCENT_BLACK)
                .opaque(true)
                .build();
    }
    public static JTextArea createBlackTitleTextArea(String text) {
        return createTextAreaBuilder()
                .fontStyle(Font.BOLD)
                .fontSize(50)
                .text(text)
                .foreground(Color.BLACK)
                .build();
    }
    public static TextAreaBuilder createTextAreaBuilder() {
        return new TextAreaBuilder();
    }

    public static class TextAreaBuilder {
        private int fontStyle = Font.PLAIN;
        private int fontSize = 26;
        private Color foreground = Color.WHITE;
        private Color background = GameConstants.COLOR_TRANSPARENT;
        private String text = "";
        private boolean focusable = false;
        private boolean opaque = DEFAULT_OPAQUE;

        public TextAreaBuilder fontStyle(int fontStyle) { this.fontStyle = fontStyle; return this; }
        public TextAreaBuilder fontSize(int fontSize) { this.fontSize = fontSize; return this; }
        public TextAreaBuilder foreground(Color foreground) { this.foreground = foreground; return this; }
        public TextAreaBuilder background(Color background) { this.background = background; return this; }
        public TextAreaBuilder text(String text) { this.text = text; return this; }
        public TextAreaBuilder focusable(boolean focusable) { this.focusable = focusable; return this; }
        public TextAreaBuilder opaque(boolean opaque) { this.opaque = opaque; return this; }

        public JTextArea build() {
            JTextArea textArea = new JTextArea(text);
            textArea.setEditable(DEFAULT_EDITABLE);
            textArea.setLineWrap(DEFAULT_LINE_WRAP);
            textArea.setWrapStyleWord(DEFAULT_WRAP_STYLE_WORD);
            textArea.setBorder(DEFAULT_BORDER);
            textArea.setFont(new Font(GameConstants.FONT_FAMILY, fontStyle, fontSize));
            textArea.setForeground(foreground);
            textArea.setBackground(background);
            textArea.setOpaque(this.opaque);
            textArea.setFocusable(false);
            textArea.setHighlighter(null);
            return textArea;
        }
    }
}

// ==================== 常量类（与原版保持兼容）====================
@Deprecated
final class ButtonConst {
    static final int Simple_Button = 0;
    static final int Draggable_Button = 1;
}
@Deprecated
final class LabelConst {
    static final int Simple_Label = 0;
    static final int Text_Label = 1;
    static final int Black_Label = 2;
}
@Deprecated
final class PanelConst {
    static final int Simple_Panel = 0;
}
