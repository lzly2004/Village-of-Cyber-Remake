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
            case LabelConst.Black_Label ->
            {
                return createBlackLabel(text,icon);
            }
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
        jLabel.setFont(new Font(GameConstants.FONT_FAMILY, Font.PLAIN, GameConstants.FONT_SIZE_DIALOG));
        jLabel.setOpaque(false);
        return jLabel;
    }
    public static JLabel createBlackLabel(String text,ImageIcon icon)    //创建一个普通按钮
    {
        JLabel jLabel = new JLabel();
        if(icon != null)
            jLabel.setIcon(icon);
        if(text != null)
            jLabel.setText(text);
        jLabel.setOpaque(false);// 1. 设置标签背景透明：只显示文字/图片，不会用默认背景色遮挡下方组件
        jLabel.setForeground(Color.BLACK); // 2. 设置标签文字颜色为黑色
        // 3. 设置标签字体：
        //    - 字体名：Takao Mincho（一款日文宋体，适合显示日文/中文）
        //    - 样式：Font.BOLD（加粗）
        //    - 字号：50号（大字号，适合标题/醒目的文字）
        jLabel.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, GameConstants.FONT_SIZE_TITLE));
        return jLabel;
    }

    //扩展：返回带图片的，确定坐标与大小的图标
    public static JLabel makeLabel(int kind,int x,int y,int width,int height,ImageIcon icon)
    {
        return makeLabel(kind,x,y,width,height,null,icon);
    }
    public static JLabel makeLabel(int kind,int x,int y,int width,int height,String text)
    {
        return makeLabel(kind,x,y,width,height,text,null);
    }
    public static JLabel makeLabel(int kind,int x,int y,ImageIcon icon)
    {
        return makeLabel(kind,x,y,icon.getIconWidth(),icon.getIconHeight(),null,icon);
    }
    public static JLabel makeLabel(int kind,int x,int y,int width,int height,String text,ImageIcon icon)
    {
        switch (kind)
        {
            case LabelConst.Simple_Label ->
            {
                return createSimpleBoundedLabel(x,y,width,height,text,icon);
            }
            case LabelConst.Text_Label ->
            {
                return createTextBoundedLabel(x,y,width,height,text,icon);
            }
            case LabelConst.Black_Label ->
            {
                return createBlackBoundedLabel(x,y,width,height,text,icon);
            }
        }
        return null;
    }
    public static JLabel createSimpleBoundedLabel(int x,int y,int width,int height,String text,ImageIcon icon)    //创建一个普通按钮
    {
        JLabel jLabel = new JLabel();
        jLabel.setBounds(x,y,width,height);
        if(icon != null)
        {
            ImageIcon scaledicon = ConstNum.scaleIcon(icon, width, height);
            jLabel.setIcon(scaledicon);
        }
        if(text != null)
            jLabel.setText(text);
        jLabel.setOpaque(false);
        jLabel.setFocusable(false);
        return jLabel;
    }
    public static JLabel createTextBoundedLabel(int x,int y,int width,int height,String text,ImageIcon icon)    //创建一个普通按钮
    {
        JLabel jLabel = new JLabel();
        jLabel.setBounds(x,y,width,height);
        if(icon != null)
        {
            ImageIcon scaledicon = ConstNum.scaleIcon(icon, width, height);
            jLabel.setIcon(scaledicon);
        }
        if(text != null)
            jLabel.setText(text);
        jLabel.setForeground(Color.WHITE);
        jLabel.setFont(new Font(GameConstants.FONT_FAMILY, Font.PLAIN, GameConstants.FONT_SIZE_DIALOG));
        jLabel.setOpaque(false);
        return jLabel;
    }
    public static JLabel createBlackBoundedLabel(int x,int y,int width,int height,String text,ImageIcon icon)    //创建一个普通按钮
    {
        JLabel jLabel = new JLabel();
        jLabel.setBounds(x,y,width,height);
        if(icon != null)
        {
            ImageIcon scaledicon = ConstNum.scaleIcon(icon, width, height);
            jLabel.setIcon(scaledicon);
        }
        if(text != null)
            jLabel.setText(text);
        jLabel.setOpaque(false);// 1. 设置标签背景透明：只显示文字/图片，不会用默认背景色遮挡下方组件
        jLabel.setForeground(Color.BLACK); // 2. 设置标签文字颜色为黑色
        // 3. 设置标签字体：
        //    - 字体名：Takao Mincho（一款日文宋体，适合显示日文/中文）
        //    - 样式：Font.BOLD（加粗）
        //    - 字号：50号（大字号，适合标题/醒目的文字）
        jLabel.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, GameConstants.FONT_SIZE_TITLE));
        return jLabel;
    }
}
final class LabelConst//按钮常量类
{
    static final int Simple_Label = 0;//普通标签
    static final int Text_Label = 1;//文本标签
    static final int Black_Label = 2;//普通标签，带文本
}