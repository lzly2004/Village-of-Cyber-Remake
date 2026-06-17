import javax.swing.*;
import java.awt.*;

public class DialogueDayHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        Event event = ui.events.poll();
        if (event == null) {
            ui.currentScene = UI.Scene.GAME_SCENE_VOTE;
            ui.run();
            return;
        }
        boolean isConnect = true;
        switch (event.eventname) {
            case jhdh8b:
            case gyfo1r:
            case qfjcqr5r:
            case gprz11p:
            case jbdh8r:
            case gprz11r:
            case zcrh12r:
                if (DebugLogger.getInstance().isEnabled()) {
                    DebugLogger.log("******************不变");
                }
                break;
            default:
                isConnect = false;
                break;
        }
        if (!ui.linkIcon.isEmpty() && !isConnect) {
            if (DebugLogger.getInstance().isEnabled()) {
                DebugLogger.log("进入了isConnect");
            }
            ui.linkIcon.remove(0);
        }
        DialogueBox.Components dc = DialogueBox.setup(ui, "haikei3.png");
        JLabel nameLabel = LabelSimpleFactory.makeLabel(LabelConst.Text_Label, 40, 10, 1000, 30,
                ui.uiComponentFactory.getCharacterFullName(event.ch1));
        dc.dialogPanel.add(nameLabel);
        String text = ui.resources.getEventText(event);
        Timer typeTimer = UIHelpers.bindTypewriter(dc.dialogText, text, dc.nextBtn, () -> {
            if (ui.events.isEmpty()) {
                ui.linkIcon.clear();
                ui.currentScene = UI.Scene.GAME_SCENE_VOTE;
            }
            ui.run();
        });
        dc.dialogPanel.setVisible(true);
        ImageIcon[] CharIcon = ui.resources.getEventImage(event);
        JLabel Chara = new JLabel();
        if (!ui.linkIcon.isEmpty()) {
            if (!ui.specialEvent[0]) {
                if (DebugLogger.getInstance().isEnabled()) {
                    DebugLogger.log("**********不为空且不是特殊事件");
                }
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
                if (event.eventname == EventName.gprz11r) {
                    DebugLogger.log("共有认证rrrr");
                    if (ui.events.getFirst().eventname == EventName.gprz11p) {
                        if (DebugLogger.getInstance().isEnabled()) {
                            DebugLogger.log("共有认证pppp");
                        }
                        ui.linkIcon.add(CharIcon[0]);
                        ui.specialEvent[0] = true;
                    } else {
                        if (DebugLogger.getInstance().isEnabled()) {
                            DebugLogger.log("共有认证失败");
                        }
                    }
                }
            } else {
                Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 650,
                        720 - ui.linkIcon.get(0).getIconHeight() - 30,
                        ui.linkIcon.get(0).getIconWidth(), ui.linkIcon.get(0).getIconHeight(),
                        ui.linkIcon.get(0));
                ui.diaPanel.add(Chara);
                ui.resizeComponents();
                JLabel Chara2 = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 300,
                        720 - CharIcon[0].getIconHeight() - 30,
                        CharIcon[0].getIconWidth(), CharIcon[0].getIconHeight(), CharIcon[0]);
                ui.diaPanel.add(Chara2);
                ui.diaPanel.setComponentZOrder(Chara2, 1);
                ui.linkIcon.remove(0);
                ui.specialEvent[0] = false;
            }
        } else {
            if (DebugLogger.getInstance().isEnabled()) {
                DebugLogger.log("linkIcon是空");
            }
            switch (event.eventname) {
                case gyfo1:
                case qfjc5:
                case zjgh8b:
                case zjgb8:
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