// DialogueAfternoonHandler.java - 下午对话场景处理器（用于回避CO）
import javax.swing.*;
import java.awt.*;

public class DialogueAfternoonHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        Event event = ui.events.poll();
        ui.diaPanel.removeAll();
        ui.diaPanel.setVisible(true);
        JLabel background = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT,
                ui.resources.getImage("haikei.png"));
        ImageIcon backIcon = ui.resources.getImage("messageframe.png");
        JPanel dialogPanel = PanelSimpleFactory.createSimplePanel(260, 450, 760, 230, false, true);
        ui.diaPanel.add(dialogPanel);
        JLabel back = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0, 760, 230, backIcon);
        JLabel nameLabel = LabelSimpleFactory.makeLabel(LabelConst.Text_Label, 40, 10, 1000, 30,
                ui.uiComponentFactory.getCharacterFullName(event.ch1));
        dialogPanel.add(nameLabel);
        JTextArea dialogText = TextareaSimpleFactory.createBasicTextArea(Color.WHITE);
        dialogText.setBounds(20, 50, 710, 200);
        JButton nextBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 0, 0, 760, 230, null, null);
        String text = ui.resources.getEventText(event);
        ui.bindTypewriter(dialogText, text, nextBtn, () -> {
            if (ui.events.isEmpty()) {
                ui.currentScene = UI.Scene.GAME_SCENE_VOTE;
            }
            ui.run();
        });
        dialogPanel.setVisible(true);
        ImageIcon[] CharIcon = ui.resources.getEventImage(event);
        JLabel Chara = new JLabel();
        if (!ui.linkIcon.isEmpty()) {
            Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 650,
                    720 - CharIcon[0].getIconHeight() - 30,
                    CharIcon[0].getIconWidth(), CharIcon[0].getIconHeight(), CharIcon[0]);
            ui.diaPanel.add(Chara);
            ui.resizeComponents();
            JLabel Chara2 = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 300,
                    720 - ui.linkIcon.get(0).getIconHeight() - 30,
                    ui.linkIcon.get(0).getIconWidth(), ui.linkIcon.get(0).getIconHeight(),
                    ui.linkIcon.get(0));
            ui.diaPanel.add(Chara2);
            ui.diaPanel.setComponentZOrder(Chara2, 1);
            ui.linkIcon.remove(0);
        } else {
            switch (event.eventname) {
                case gyfo1:
                case qfjc5:
                case zjgh8b:
                case zjgb8:
                case gprz11p:
                case zcrh12:
                    Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 300,
                            720 - CharIcon[0].getIconHeight() - 30,
                            CharIcon[0].getIconWidth(), CharIcon[0].getIconHeight(), CharIcon[0]);
                    ui.linkIcon.add(CharIcon[0]);
                    break;
                default:
                    Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                            (1280 - CharIcon[0].getIconWidth()) / 2,
                            720 - CharIcon[0].getIconHeight() - 30,
                            CharIcon[0].getIconWidth(), CharIcon[0].getIconHeight(), CharIcon[0]);
                    break;
            }
            ui.diaPanel.add(Chara);
            ui.resizeComponents();
        }
        dialogPanel.add(nextBtn);
        dialogPanel.add(dialogText);
        dialogPanel.add(back);
        ui.diaPanel.add(background);
        ui.jPanel.add(ui.diaPanel);
        ui.jPanel.setComponentZOrder(ui.diaPanel, 0);
        ui.diaPanel.setVisible(true);
        ui.resizeComponents();
    }
}
