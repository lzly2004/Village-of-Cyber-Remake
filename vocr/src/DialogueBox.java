import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.LinkedList;
import javax.sound.sampled.*;
import java.util.List;
import javax.swing.*;
import java.awt.*;

public class DialogueBox
{
    private static int delay = 50;//对话框播放内容的延迟
    private Runnable onComplete;//设置回调
    String text1,text2,text3;//一个事件的文本内容
    JPanel panel;//组件被添加到的主面板(jPanel)
    //void play(String text1,String text2,String text3);
    DialogueBox(JPanel jPanel)
    {
        panel = jPanel;
    }

}
