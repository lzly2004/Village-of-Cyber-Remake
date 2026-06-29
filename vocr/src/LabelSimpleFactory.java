import javax.swing.*;
import java.awt.*;

public class LabelSimpleFactory
{
    public static JLabel makeLabel(int kind, String text,ImageIcon icon)
    {
        switch (kind)
        {
            case LabelConst.Simple_Label -> { return createSimpleLabel(text,icon); }
            case LabelConst.Text_Label -> { return createTextLabel(text,icon); }
            case LabelConst.Black_Label -> { return createBlackLabel(text,icon); }
        }
        return null;
    }
    public static JLabel makeLabel(int kind, ImageIcon icon)
    {
      return makeLabel(kind,null,icon);
    }
    public static JLabel makeLabel(int kind, String text)
    {
       return makeLabel(kind,text,null);
    }
    public static JLabel createSimpleLabel(String text,ImageIcon icon)
    {
        return createLabel(text, icon, LabelConst.Simple_Label);
    }
    public static JLabel createTextLabel(String text,ImageIcon icon)
    {
        return createLabel(text, icon, LabelConst.Text_Label);
    }
    public static JLabel createBlackLabel(String text,ImageIcon icon)
    {
        return createLabel(text, icon, LabelConst.Black_Label);
    }

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
            case LabelConst.Simple_Label -> { return createSimpleBoundedLabel(x,y,width,height,text,icon); }
            case LabelConst.Text_Label -> { return createTextBoundedLabel(x,y,width,height,text,icon); }
            case LabelConst.Black_Label -> { return createBlackBoundedLabel(x,y,width,height,text,icon); }
        }
        return null;
    }
    public static JLabel createSimpleBoundedLabel(int x,int y,int width,int height,String text,ImageIcon icon)
    {
        return createBoundedLabel(x,y,width,height,text,icon,LabelConst.Simple_Label);
    }
    public static JLabel createTextBoundedLabel(int x,int y,int width,int height,String text,ImageIcon icon)
    {
        return createBoundedLabel(x,y,width,height,text,icon,LabelConst.Text_Label);
    }
    public static JLabel createBlackBoundedLabel(int x,int y,int width,int height,String text,ImageIcon icon)
    {
        return createBoundedLabel(x,y,width,height,text,icon,LabelConst.Black_Label);
    }

    private static JLabel createLabel(String text, ImageIcon icon, int kind)
    {
        JLabel label = new JLabel();
        if(icon != null) label.setIcon(icon);
        if(text != null) label.setText(text);
        applyLabelStyle(label, kind);
        return label;
    }

    private static JLabel createBoundedLabel(int x,int y,int width,int height,String text,ImageIcon icon,int kind)
    {
        JLabel label = new JLabel();
        label.setBounds(x,y,width,height);
        if(icon != null) label.setIcon(ConstNum.scaleIcon(icon, width, height));
        if(text != null) label.setText(text);
        applyLabelStyle(label, kind);
        return label;
    }

    private static void applyLabelStyle(JLabel label, int kind)
    {
        label.setOpaque(false);
        switch (kind)
        {
            case LabelConst.Simple_Label -> label.setFocusable(false);
            case LabelConst.Text_Label ->
            {
                label.setForeground(Color.WHITE);
                label.setFont(new Font(GameConstants.FONT_FAMILY, Font.PLAIN, GameConstants.FONT_SIZE_DIALOG));
            }
            case LabelConst.Black_Label ->
            {
                label.setForeground(Color.BLACK);
                label.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, GameConstants.FONT_SIZE_TITLE));
            }
        }
    }
}
final class LabelConst
{
    static final int Simple_Label = 0;
    static final int Text_Label = 1;
    static final int Black_Label = 2;
}