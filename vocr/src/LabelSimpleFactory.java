import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LabelSimpleFactory
{
    public static JLabel makeLabel(int kind, String text,ImageIcon icon)//暂时放在这里，没来得及添加参数
    {
        switch (kind)
        {
            case LabelConst.Simple_Label ->
            {
                return createSimpleLabel(text,icon);
            }
            case LabelConst.Text_Label ->
            {
                return createTextLabel(text,icon);
            }
            /*case ButtonConst.Draggable_Button -> {
                return createDraggableButton(null, icon);
            }
            case ButtonConst.SimpleTextButton ->//暂时放在这里，没来得及添加参数
            {
                return createSimpleTextButton(null,icon);
            }*/
        }
        return null;
    }
    public static JLabel makeLabel(int kind, ImageIcon icon)//暂时放在这里，没来得及添加参数
    {
      return makeLabel(kind,null,icon);
    }
    public static JLabel makeLabel(int kind, String text)//暂时放在这里，没来得及添加参数
    {
       return makeLabel(kind,text,null);
    }
    /*
    public static JButton makeButton(int kind, String text, ImageIcon icon)//暂时放在这里，没来得及添加参数
    {
        switch (kind) {
            case ButtonConst.Simple_Button -> {
                return createSimpleButton(text, icon);
            }
            case ButtonConst.Draggable_Button -> {
                return createDraggableButton(text, icon);
            }
        }
        return null;
    }
    public static void setButton(JButton btn)   //设置按钮为普通按钮基础设置
    {
        btn.setOpaque(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setForeground(Color.white);
        btn.setFont(new Font("Takao Mincho",Font.BOLD,20));
        btn.setFocusPainted(false);
    }
    public static JButton createDraggableButton(String text,ImageIcon icon) //创建一个拖动button:指定的时候使用
    {
        // 1. 创建基础空按钮
        JButton draggableBtn = createSimpleButton(text,icon);
        draggableBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // 默认大小（方便拖拽，可按需删除或调整）
        draggableBtn.setPreferredSize(new Dimension(80, 30));
        // 记录核心数据：初始位置（拖拽前的位置，用于松开后回位）
        final Point initPos = new Point();
        // 记录鼠标相对于按钮的偏移量
        final int[] mouseOffset = new int[2];

        // 2. 鼠标按下：记录初始位置+偏移量+切换光标+按钮置顶
        draggableBtn.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                // 记录按钮当前位置（拖拽前的初始位置）
                initPos.setLocation(draggableBtn.getX(), draggableBtn.getY());
                // 记录鼠标在按钮内的偏移量（防止拖动瞬移）
                mouseOffset[0] = e.getX();
                mouseOffset[1] = e.getY();
                // 切换为移动光标
                draggableBtn.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                // 按钮置顶，避免被其他组件遮挡
                Container parent = draggableBtn.getParent();
                if (parent != null) {
                    parent.setComponentZOrder(draggableBtn, 0);
                    parent.repaint();
                }
            }
        });

        // 3. 鼠标拖动：跟随移动+边界限制（不超出父容器）
        draggableBtn.addMouseMotionListener(new MouseAdapter()
        {
            @Override
            public void mouseDragged(MouseEvent e)
            {
                Container parent = draggableBtn.getParent();
                // 校验父容器尺寸有效（避免缩放时计算错乱）
                if (parent == null || parent.getWidth() <= 0 || parent.getHeight() <= 0
                        || draggableBtn.getWidth() <= 0 || draggableBtn.getHeight() <= 0)
                {
                    return;
                }

                Point screenPos = e.getLocationOnScreen();
                SwingUtilities.convertPointFromScreen(screenPos, parent);

                int newX = screenPos.x - mouseOffset[0];
                int newY = screenPos.y - mouseOffset[1];

                // 边界限制（同上）
                newX = Math.max(0, Math.min(newX, parent.getWidth() - draggableBtn.getWidth()));
                newY = Math.max(0, Math.min(newY, parent.getHeight() - draggableBtn.getHeight()));

                draggableBtn.setBounds(newX, newY, draggableBtn.getWidth(), draggableBtn.getHeight());
            }
        });

        return draggableBtn;
    }
    */
    public static JLabel createSimpleLabel(String text,ImageIcon icon)    //创建一个普通按钮
    {
        JLabel jLabel = new JLabel();
        if(icon != null)
            jLabel.setIcon(icon);
        if(text != null)
            jLabel.setText(text);
        jLabel.setOpaque(false);
        jLabel.setFocusable(false);
        return jLabel;
    }
    public static JLabel createTextLabel(String text,ImageIcon icon)    //创建一个普通按钮
    {
        JLabel jLabel = new JLabel();
        if(icon != null)
            jLabel.setIcon(icon);
        if(text != null)
            jLabel.setText(text);
        jLabel.setForeground(Color.WHITE);
        jLabel.setFont(new Font("Takao Mincho", Font.PLAIN, 26));
        jLabel.setOpaque(false);
        return jLabel;
    }
}
final class LabelConst//按钮常量类
{
    static final int Simple_Label = 0;//普通标签
    static final int Text_Label = 1;//文本标签
    //static final int SimpleTextButton = 2;//普通按钮，带文本
}