import javax.swing.*;
import java.awt.event.ActionListener;

public class UIHelpers {

    /** 批量隐藏按钮 */
    public static void hideButtons(JButton... buttons) {
        for (JButton b : buttons) b.setVisible(false);
    }

    /** 打字机效果：逐字显示文本，nextBtn点击时跳到末尾或触发回调 */
    public static Timer bindTypewriter(JTextArea target, String fullText,
                                       JButton nextBtn, Runnable onComplete) {
        final int[] index = {0};
        Timer timer = new Timer(GameConstants.TYPEWRITER_DELAY_MS, e -> {
            if (index[0] < fullText.length()) {
                target.append(String.valueOf(fullText.charAt(index[0])));
                index[0]++;
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
        timer.start();
        nextBtn.addActionListener(e -> {
            if (index[0] < fullText.length()) {
                target.setText(fullText);
                index[0] = fullText.length();
                timer.stop();
            } else {
                onComplete.run();
            }
        });
        return timer;
    }
}