import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class PanelSimpleFactory
{
    public static JPanel makePanel(int kind,int width,int height,boolean isOpaque,boolean isWhiteBk)//暂时放在这里，没来得及添加参数
    {
        switch (kind)
        {
            case LabelConst.Simple_Label ->
            {
                return createSimplePanel(width,height,isOpaque,isWhiteBk);
            }
        }
        return null;
    }
    public static JPanel createSimplePanel(int width,int height,boolean isOpaque,boolean isWhiteBk)    //创建一个普通按钮
    {
       JPanel jPanel =  new JPanel();
       jPanel.setLayout(null);
       if(width > 0 && height > 0)
       {
           jPanel.setSize(width,height);
       }
       if(isWhiteBk)
           jPanel.setBackground(GameConstants.COLOR_TRANSPARENT);
       jPanel.setOpaque(isOpaque);
       return jPanel;
    }

    //扩展：可以设置位置和长宽
    public static JPanel makePanel(int kind,int x,int y,int width,int height,boolean isOpaque,boolean isWhiteBk)//暂时放在这里，没来得及添加参数
    {
        switch (kind)
        {
            case LabelConst.Simple_Label ->
            {
                return createSimplePanel(x,y,width,height,isOpaque,isWhiteBk);
            }
        }
        return null;
    }
    public static JPanel createSimplePanel(int x,int y,int width,int height,boolean isOpaque,boolean isWhiteBk)    //创建一个普通按钮
    {
        JPanel jPanel =  new JPanel();
        jPanel.setLayout(null);
        if(width > 0 && height > 0)
        {
            jPanel.setBounds(x,y,width,height);
        }
        if(isWhiteBk)
            jPanel.setBackground(GameConstants.COLOR_TRANSPARENT);
        jPanel.setOpaque(isOpaque);
        return jPanel;
    }
}
final class PanelConst//按钮常量类
{
    static final int Simple_Panel = 0;//普通面板
}
