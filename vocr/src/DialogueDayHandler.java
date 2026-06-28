import javax.swing.*;
import java.awt.*;

public class DialogueDayHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        Event event = ui.getEvents().poll();
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
                    DebugLogger.log("******************不变");
                break;
            default:
                isConnect = false;
                break;
        }
        if (!ui.linkIcon.isEmpty() && !isConnect) {
                DebugLogger.log("进入了isConnect");
            ui.linkIcon.remove(0);
        }
        UIHelpers.DialogueSetup ds = UIHelpers.prepareDialogueEvent(ui, "haikei3.png", event);
        Timer typeTimer = UIHelpers.bindTypewriter(ds.dc().dialogText, ds.text(), ds.dc().nextBtn, () -> {
            if (ui.getEvents().isEmpty()) {
                ui.linkIcon.clear();
                ui.currentScene = UI.Scene.GAME_SCENE_VOTE;
            }
            ui.run();
        });
        ds.dc().dialogPanel.setVisible(true);
        ImageIcon[] CharIcon = ds.charIcon();
        JLabel Chara = new JLabel();
        if (!ui.linkIcon.isEmpty()) {
            if (!ui.specialEvent[0]) {
                    DebugLogger.log("**********不为空且不是特殊事件");
                UIHelpers.renderLinkIconPair(ui, CharIcon);
                if (event.eventname == EventName.gprz11r) {
                    DebugLogger.log("共有认证rrrr");
                    if (ui.getEvents().getFirst().eventname == EventName.gprz11p) {
                            DebugLogger.log("共有认证pppp");
                        ui.linkIcon.add(CharIcon[0]);
                        ui.specialEvent[0] = true;
                    } else {
                        DebugLogger.log("共有认证失败");
                    }
                }
            } else {
                Chara = UIFactory.makeLabel(LabelConst.Simple_Label, 650,
                        GameConstants.WINDOW_HEIGHT - ui.linkIcon.get(0).getIconHeight() - GameConstants.CHAR_ICON_BOTTOM_MARGIN,
                        ui.linkIcon.get(0).getIconWidth(), ui.linkIcon.get(0).getIconHeight(),
                        ui.linkIcon.get(0));
                ui.diaPanel.add(Chara);
                ui.resizeComponents();
                JLabel Chara2 = UIFactory.makeLabel(LabelConst.Simple_Label, 300,
                        GameConstants.WINDOW_HEIGHT - CharIcon[0].getIconHeight() - GameConstants.CHAR_ICON_BOTTOM_MARGIN,
                        CharIcon[0].getIconWidth(), CharIcon[0].getIconHeight(), CharIcon[0]);
                ui.diaPanel.add(Chara2);
                ui.diaPanel.setComponentZOrder(Chara2, 1);
                ui.linkIcon.remove(0);
                ui.specialEvent[0] = false;
            }
        } else {
                DebugLogger.log("linkIcon是空");
            UIHelpers.renderDialogueCharacter(ui, event, CharIcon);
        }
        DialogueBox.finalize(ui, ds.dc());
    }
}