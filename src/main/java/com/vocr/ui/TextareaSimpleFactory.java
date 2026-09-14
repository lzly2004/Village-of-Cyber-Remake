package com.vocr.ui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * JTextArea 简单工厂类：一行代码创建游戏所需文本区域，统一配置核心样式，减少冗余
 */
final class TextareaSimpleFactory
{
    // 核心固定配置（所有类型共享，如需修改统一调整）
    private static final boolean DEFAULT_EDITABLE = false; // 不可编辑
    private static final boolean DEFAULT_LINE_WRAP = true; // 自动换行
    private static final boolean DEFAULT_WRAP_STYLE_WORD = true; // 按单词拆分
    private static final boolean DEFAULT_OPAQUE = false; // 背景透明
    private static final Border DEFAULT_BORDER = BorderFactory.createEmptyBorder(); // 无边框
    // 三种在用样式的明确入口；不暴露没有消费者的通用 Builder。
    /**
     * 基础显示型：常规字体+26号+白色文字+全透明背景（游戏对话、普通正文）
     */
    static JTextArea createBasicTextArea(Color color)
    {
        return createTextArea(Font.PLAIN, 26, color,
                UIConstants.COLOR_TRANSPARENT, "", DEFAULT_OPAQUE);
    }

     /*
      * 加粗标题型：加粗字体+指定字号+白色文字+全透明背景（提示、规则、统计数据）
      * @param fontSize 字号（支持16/20/24，对应原有场景）
      * @param text 初始文本（可选，无则传空字符串）
      */
    static JTextArea createBoldTitleTextArea(Color color,int fontSize, String text)
    {
        return createTextArea(Font.BOLD, fontSize, color,
                UIConstants.COLOR_TRANSPARENT, text, DEFAULT_OPAQUE);
    }
    /**
     * 半透明背景型：加粗字体+24号+白色文字+半透明背景（重要提示、弹窗）
     * @param text 初始提示文本
     */
    static JTextArea createTranslucentTipTextArea(String text)
    {
        return createTextArea(Font.BOLD, 24, Color.WHITE,
                UIConstants.COLOR_TRANSLUCENT_BLACK, text, true);
    }

    private static JTextArea createTextArea(
            int fontStyle, int fontSize, Color foreground,
            Color background, String text, boolean opaque)
    {
        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(DEFAULT_EDITABLE);
        textArea.setLineWrap(DEFAULT_LINE_WRAP);
        textArea.setWrapStyleWord(DEFAULT_WRAP_STYLE_WORD);
        textArea.setBorder(DEFAULT_BORDER);
        textArea.setFont(new Font(UIConstants.FONT_FAMILY, fontStyle, fontSize));
        textArea.setForeground(foreground);
        textArea.setBackground(background);
        textArea.setOpaque(opaque);
        textArea.setFocusable(false);
        textArea.setHighlighter(null);
        return textArea;
    }
}
