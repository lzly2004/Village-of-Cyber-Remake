import javax.swing.*;
import java.awt.*;

public class DialogueBox
{
    private static int delay = 50;
    private Runnable onComplete;
    String text1,text2,text3;
    JPanel panel;

    DialogueBox(JPanel jPanel)
    {
        panel = jPanel;
    }

    public static class Components
    {
        public JLabel background;
        public JPanel dialogPanel;
        public JLabel back;
        public JTextArea dialogText;
        public JButton nextBtn;
    }

    public static Components create(ResourcesInterface resources, String bgImageName)
    {
        Components c = new Components();
        c.background = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT,
                resources.getImage(bgImageName));
        ImageIcon backIcon = resources.getImage("messageframe.png");
        c.dialogPanel = PanelSimpleFactory.createSimplePanel(260, 450,
                backIcon.getIconWidth(), backIcon.getIconHeight(), false, true);
        c.back = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                backIcon.getIconWidth(), backIcon.getIconHeight(), backIcon);
        c.dialogText = TextareaSimpleFactory.createBasicTextArea(Color.WHITE);
        c.dialogText.setBounds(20, 50, 710, 200);
        c.nextBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 0, 0,
                backIcon.getIconWidth(), backIcon.getIconHeight(), null, null);
        return c;
    }

    public static Components createWithIcon(ResourcesInterface resources, ImageIcon bgIcon)
    {
        Components c = new Components();
        c.background = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT, bgIcon);
        ImageIcon backIcon = resources.getImage("messageframe.png");
        c.dialogPanel = PanelSimpleFactory.createSimplePanel(260, 450,
                backIcon.getIconWidth(), backIcon.getIconHeight(), false, true);
        c.back = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                backIcon.getIconWidth(), backIcon.getIconHeight(), backIcon);
        c.dialogText = TextareaSimpleFactory.createBasicTextArea(Color.WHITE);
        c.dialogText.setBounds(20, 50, 710, 200);
        c.nextBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 0, 0,
                backIcon.getIconWidth(), backIcon.getIconHeight(), null, null);
        return c;
    }

    public static Components setup(UI ui, String bgImageName)
    {
        ui.diaPanel.removeAll();
        ui.diaPanel.setVisible(true);
        Components c = create(ui.resources, bgImageName);
        ui.diaPanel.add(c.dialogPanel);
        return c;
    }

    public static Components setupWithIcon(UI ui, ImageIcon bgIcon)
    {
        ui.diaPanel.removeAll();
        ui.diaPanel.setVisible(true);
        Components c = createWithIcon(ui.resources, bgIcon);
        ui.diaPanel.add(c.dialogPanel);
        return c;
    }

    public static void finalize(UI ui, Components c)
    {
        c.dialogPanel.add(c.nextBtn);
        c.dialogPanel.add(c.dialogText);
        c.dialogPanel.add(c.back);
        ui.diaPanel.add(c.background);
        ui.jPanel.add(ui.diaPanel);
        ui.jPanel.setComponentZOrder(ui.diaPanel, 0);
        ui.diaPanel.setVisible(true);
        ui.resizeComponents();
    }
}