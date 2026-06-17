// DialogueAfternoonHandler.java - 下午对话场景处理器（用于回避CO）
import javax.swing.*;
import java.awt.*;

public class DialogueAfternoonHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        Event event = ui.events.poll();
        DialogueBox.Components dc = DialogueBox.setup(ui, "haikei.png");
        JLabel nameLabel = LabelSimpleFactory.makeLabel(LabelConst.Text_Label, 40, 10, 1000, 30,
                ui.uiComponentFactory.getCharacterFullName(event.ch1));
        dc.dialogPanel.add(nameLabel);
        String text = ui.resources.getEventText(event);
        UIHelpers.bindTypewriter(dc.dialogText, text, dc.nextBtn, () -> {
            if (ui.events.isEmpty()) {
                ui.currentScene = UI.Scene.GAME_SCENE_VOTE;
            }
            ui.run();
        });
        dc.dialogPanel.setVisible(true);
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
        DialogueBox.finalize(ui, dc);
    }
}