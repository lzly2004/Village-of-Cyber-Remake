// 文件: UITextEffect.java

import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * 文本效果工具类 - 处理逐字打印等文本动画效果
 */
public class UITextEffect
{

    /**
     * 创建逐字打印效果
     * @param textArea 目标文本区域
     * @param fullText 要显示的完整文本
     * @param delay 每个字符的延迟(毫秒)
     * @param onComplete 打印完成时的回调(可为null)
     * @return 定时器对象，可用于控制(停止或提前完成)
     */
    public static Timer startTypewriter(JTextArea textArea, String fullText,
                                        int delay, Runnable onComplete) {
        textArea.setText(""); // 清空现有文本

        final int[] index = {0};
        Timer timer = new Timer(delay, e -> {
            if (index[0] < fullText.length()) {
                textArea.append(String.valueOf(fullText.charAt(index[0])));
                index[0]++;
            } else {
                ((Timer) e.getSource()).stop();
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
        timer.start();
        return timer;
    }

    /**
     * 立即完成逐字打印效果
     * @param textArea 目标文本区域
     * @param fullText 完整的文本
     * @param timer 对应的定时器(可为null)
     */
    public static void completeTypewriter(JTextArea textArea, String fullText, Timer timer) {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
        textArea.setText(fullText);
    }

    /**
     * 检查打印是否完成
     * @param textArea 文本区域
     * @param fullText 完整文本
     * @return 是否已完成打印
     */
    public static boolean isTypewriterComplete(JTextArea textArea, String fullText) {
        return textArea.getText().length() >= fullText.length();
    }

    /**
     * 创建并立即启动逐字打印的便捷方法
     */
    public static Timer startTypewriter(JTextArea textArea, String fullText) {
        return startTypewriter(textArea, fullText, 50, null);
    }
}