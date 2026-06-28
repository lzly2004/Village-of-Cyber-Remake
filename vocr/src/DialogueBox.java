import javax.swing.*;
import java.awt.*;

public class DialogueBox
{
    DialogueBox(JPanel jPanel)
    {
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
        return createWithIcon(resources, resources.getImage(bgImageName));
    }

    public static Components createWithIcon(ResourcesInterface resources, ImageIcon bgIcon)
    {
        Components c = new Components();
        c.background = UIFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT, bgIcon);
        ImageIcon backIcon = resources.getImage("messageframe.png");
        c.dialogPanel = UIFactory.createSimplePanel(260, 450,
                backIcon.getIconWidth(), backIcon.getIconHeight(), false, true);
        c.back = UIFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                backIcon.getIconWidth(), backIcon.getIconHeight(), backIcon);
        c.dialogText = UIFactory.createBasicTextArea(Color.WHITE);
        c.dialogText.setBounds(20, 50, 710, 200);
        c.nextBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 0, 0,
                backIcon.getIconWidth(), backIcon.getIconHeight(), null, null);
        return c;
    }

    public static Components setup(UI ui, String bgImageName)
    {
        return setupWithIcon(ui, ui.resources.getImage(bgImageName));
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