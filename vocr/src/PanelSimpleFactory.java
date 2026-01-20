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
       if(width > 0 && height > 0)
       {
           jPanel.setSize(width,height);
       }
       if(isWhiteBk)
           jPanel.setBackground(new Color(0,0,0,0));
       jPanel.setOpaque(isOpaque);
       jPanel.setLayout(null);
       return jPanel;
    }

}
final class PanelConst//按钮常量类
{
    static final int Simple_Panel = 0;//普通面板
}
