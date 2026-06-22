import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ButtonSimpleFactory
{
    public static JButton makeButton(int kind,ImageIcon icon)//暂时放在这里，没来得及添加参数
    {
        return makeButton(kind,null,icon);
    }
    public static JButton makeButton(int kind,String text)//暂时放在这里，没来得及添加参数
    {
        return makeButton(kind,text,null);
    }
    public static JButton makeButton(int kind,String text,ImageIcon icon)//暂时放在这里，没来得及添加参数
    {
        switch(kind)
        {
            case ButtonConst.Simple_Button ->
            {
                return createSimpleButton(text,icon);
            }
            case ButtonConst.Draggable_Button ->
            {
                return createDraggableButton(text,icon);
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
        btn.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, GameConstants.FONT_SIZE_BUTTON));
        btn.setFocusPainted(false);
    }
    public static JButton createDraggableButton(String text,ImageIcon icon) //创建一个拖动button:指定的时候使用
    {
        JButton draggableBtn = createSimpleButton(text,icon);
        addDragListeners(draggableBtn);
        return draggableBtn;
    }

    private static void addDragListeners(JButton draggableBtn)
    {
        draggableBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        draggableBtn.setPreferredSize(new Dimension(80, 30));
        final Point initPos = new Point();
        final int[] mouseOffset = new int[2];

        draggableBtn.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
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

        draggableBtn.addMouseMotionListener(new MouseAdapter()
        {
            @Override
            public void mouseDragged(MouseEvent e)
            {
                Container parent = draggableBtn.getParent();
                if (parent == null || parent.getWidth() <= 0 || parent.getHeight() <= 0
                        || draggableBtn.getWidth() <= 0 || draggableBtn.getHeight() <= 0)
                {
                    return;
                }

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
    public static JButton createSimpleButton(String text,ImageIcon icon)    //创建一个普通按钮
    {
        JButton btn = new JButton();
        if(icon != null)
            btn.setIcon(icon);
        if(text != null)
            btn.setText(text);
        setButton(btn);
        return btn;
    }

    //扩展：返回带图片的，确定坐标与大小的图标
    public static JButton makeButton(int kind,int x,int y,int width,int height,ImageIcon icon)//暂时放在这里，没来得及添加参数
    {
       return  makeButton(kind,x,y,width,height,null,icon);
    }
    public static JButton makeButton(int kind,int x,int y,ImageIcon icon)//暂时放在这里，没来得及添加参数
    {
        return  makeButton(kind,x,y,icon.getIconWidth(),icon.getIconHeight(),null,icon);
    }
    public static JButton makeButton(int kind,int x,int y,int width,int height,String text,ImageIcon icon)//暂时放在这里，没来得及添加参数
    {
        switch(kind)
        {
            case ButtonConst.Simple_Button ->
            {
                return createSimpleBoundedButton(x,y,width,height,text,icon);
            }
            case ButtonConst.Draggable_Button ->
            {
                return createDraggableBoundedButton(x,y,width,height,text,icon);
            }
        }
        return null;
    }
    public static JButton createDraggableBoundedButton(int x,int y,int width,int height,String text,ImageIcon icon) //创建一个拖动button:指定的时候使用
    {
        JButton draggableBtn = createSimpleBoundedButton(x,y,width,height,text,icon);
        addDragListeners(draggableBtn);
        return draggableBtn;
    }
    public static JButton createSimpleBoundedButton(int x,int y,int width,int height,String text,ImageIcon icon)    //创建一个普通按钮
    {
        JButton btn = new JButton();
        btn.setBounds(x,y,width,height);
        if(icon != null)
        {
            ImageIcon scaledIcon = ConstNum.scaleIcon(icon, width, height);
            btn.setIcon(scaledIcon);
        }
        if(text != null)
            btn.setText(text);
        setButton(btn);
        return btn;
    }
}
final class ButtonConst//按钮常量类
{
    static final int Simple_Button = 0;//普通按钮
    static final int Draggable_Button = 1;//可拉取的按钮
}