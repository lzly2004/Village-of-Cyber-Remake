import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * JTextArea 简单工厂类：一行代码创建游戏所需文本区域，统一配置核心样式，减少冗余
 */
public class TextareaSimpleFactory
{
    // 核心固定配置（所有类型共享，如需修改统一调整）
    private static final boolean DEFAULT_EDITABLE = false; // 不可编辑
    private static final boolean DEFAULT_LINE_WRAP = true; // 自动换行
    private static final boolean DEFAULT_WRAP_STYLE_WORD = true; // 按单词拆分
    private static final boolean DEFAULT_OPAQUE = false; // 背景透明
    private static final Border DEFAULT_BORDER = BorderFactory.createEmptyBorder(); // 无边框
    // ==================== 1. 快捷方法：对应4类场景，一行调用 ====================
    /**
     * 基础显示型：常规字体+26号+白色文字+全透明背景（游戏对话、普通正文）
     */
    public static JTextArea createBasicTextArea(Color color)
    {
        return createTextAreaBuilder()
                .fontStyle(Font.PLAIN)
                .fontSize(26)
                .foreground(color)
                .build();
    }

     /*
      * 加粗标题型：加粗字体+指定字号+白色文字+全透明背景（提示、规则、统计数据）
      * @param fontSize 字号（支持16/20/24，对应原有场景）
      * @param text 初始文本（可选，无则传空字符串）
      * @param focusable 是否允许获取焦点（默认false）
      */
    public static JTextArea createBoldTitleTextArea(Color color,int fontSize, String text)
    {
        return createTextAreaBuilder()
                .fontStyle(Font.BOLD)
                .fontSize(fontSize)
                .text(text)
                .foreground(color)
                .build();
    }
    /**
     * 半透明背景型：加粗字体+24号+白色文字+半透明背景（重要提示、弹窗）
     * @param text 初始提示文本
     */
    public static JTextArea createTranslucentTipTextArea(String text)
    {
        return createTextAreaBuilder()
                .fontStyle(Font.BOLD)
                .fontSize(24)
                .text(text)
                .background(GameConstants.COLOR_TRANSLUCENT_BLACK) // 半透明背景
                .opaque(true)
                .build();
    }

    /**
     * 黑色文字型：加粗字体+50号+黑色文字+全透明背景（醒目标题）
     * @param text 标题文本
     */
    public static JTextArea createBlackTitleTextArea(String text)
    {
        return createTextAreaBuilder()
                .fontStyle(Font.BOLD)
                .fontSize(50)
                .text(text)
                .foreground(Color.BLACK) // 黑色文字
                .build();
    }

    // ==================== 2. 自定义方法：支持灵活配置（如需扩展场景） ====================
    /**
     * 自定义配置：返回建造者，支持链式调用，灵活设置所有参数
     */
    public static TextAreaBuilder createTextAreaBuilder()
    {
        return new TextAreaBuilder();
    }

    // ==================== 建造者内部类：封装配置逻辑 ====================
    public static class TextAreaBuilder
    {
        // 可变参数（默认值为核心配置）
        private int fontStyle = Font.PLAIN;
        private int fontSize = 26;
        private Color foreground = Color.WHITE;
        private Color background = GameConstants.COLOR_TRANSPARENT;
        private String text = "";
        private boolean focusable = false;
        private boolean opaque = DEFAULT_OPAQUE; // 新增：默认继承工厂类的配置
        // 链式设置方法
        public TextAreaBuilder fontStyle(int fontStyle)
        {
            this.fontStyle = fontStyle;
            return this;
        }

        public TextAreaBuilder fontSize(int fontSize)
        {
            this.fontSize = fontSize;
            return this;
        }

        public TextAreaBuilder foreground(Color foreground)
        {
            this.foreground = foreground;
            return this;
        }

        public TextAreaBuilder background(Color background)
        {
            this.background = background;
            return this;
        }

        public TextAreaBuilder text(String text)
        {
            this.text = text;
            return this;
        }

        public TextAreaBuilder focusable(boolean focusable)
        {
            this.focusable = focusable;
            return this;
        }

        // 新增：opaque 配置方法（支持链式调用）
        public TextAreaBuilder opaque(boolean opaque) {
            this.opaque = opaque;
            return this;
        }

        // 构建JTextArea实例（封装核心配置）
        public JTextArea build()
        {
            JTextArea textArea = new JTextArea(text);
            // 核心固定配置
            textArea.setEditable(DEFAULT_EDITABLE);
            textArea.setLineWrap(DEFAULT_LINE_WRAP);
            textArea.setWrapStyleWord(DEFAULT_WRAP_STYLE_WORD);
            textArea.setBorder(DEFAULT_BORDER);
            // 可变参数配置
            textArea.setFont(new Font(GameConstants.FONT_FAMILY, fontStyle, fontSize));
            textArea.setForeground(foreground);
            textArea.setBackground(background);
            //后续添加更正
            textArea.setOpaque(this.opaque);
            textArea.setFocusable(false); // 先禁用焦点（无焦点则无法触发选择）
            textArea.setHighlighter(null); // 彻底移除文本高亮器（即使有焦点也无法选中）
            return textArea;
        }
    }
}