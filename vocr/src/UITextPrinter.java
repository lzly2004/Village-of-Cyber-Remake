
import javax.swing.*;

public class UITextPrinter
{

    public static Timer createTypewriter(JTextArea textArea, final String[] fullText,
                                         final int[] index, int delay) {
        textArea.setText("");
        Timer timer = new Timer(delay, e -> {
            if (index[0] < fullText[0].length()) {
                textArea.append(String.valueOf(fullText[0].charAt(index[0])));
                index[0]++;
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
        return timer;
    }

    public static boolean isComplete(JTextArea textArea, final String[] fullText,
                                     final int[] index) {
        return index[0] >= fullText[0].length();
    }

    public static void completeImmediately(JTextArea textArea, final String[] fullText,
                                           final int[] index, Timer timer) {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
        textArea.setText(fullText[0]);
        index[0] = fullText[0].length();
    }
}