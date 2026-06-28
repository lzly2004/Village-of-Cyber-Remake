import javax.swing.*;
import java.awt.*;

public class DialogueDayDeathHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        if (ui.getEvents().getFirst().eventname == EventName.wsw) {
            ui.resources.playSound("平和音效.wav");
            DialogueBox.Components dc = DialogueBox.setup(ui, "haikei3.png");
            ui.getEvents().poll();
            UIHelpers.bindTypewriter(dc.dialogText, GameStrings.NO_SACRIFICE, dc.nextBtn, () -> {
                UIHelpers.playDayPhaseBgm(ui);
                ui.currentScene = UI.Scene.DIALOGUE_DAY;
                ui.run();
            });
            dc.dialogPanel.setVisible(true);
            DialogueBox.finalize(ui, dc);
        } else {
            Event event = ui.getEvents().poll();
            if (event == null) {
                ui.currentScene = UI.Scene.GAME_SCENE_VOTE;
                ui.run();
                return;
            }
            ui.jPanel.removeAll();
            UIHelpers.DialogueSetup ds = UIHelpers.prepareDialogueEvent(ui, "haikei3.png", event);
            ds.dc().nextBtn.setVisible(false);
            ds.dc().nextBtn.setEnabled(false);
            Timer typeTimer = UIHelpers.bindTypewriter(ds.dc().dialogText, ds.text(), ds.dc().nextBtn, () -> {
                if (ui.getEvents().isEmpty() || ui.getEvents().getFirst().eventname != EventName.yjsw) {
                    UIHelpers.playDayPhaseBgm(ui);
                    ui.currentScene = UI.Scene.DIALOGUE_DAY;
                }
                ui.run();
            });
            ds.dc().dialogPanel.setVisible(true);
            ImageIcon[] CharIcon = ds.charIcon();
            if (CharIcon.length != 0) {
                typeTimer.stop();
                ds.dc().dialogPanel.setVisible(false);
                JLabel Chara = UIFactory.makeLabel(LabelConst.Simple_Label,
                        (GameConstants.WINDOW_WIDTH - CharIcon[0].getIconWidth()) / 2,
                        GameConstants.WINDOW_HEIGHT - CharIcon[0].getIconHeight() - GameConstants.CHAR_ICON_BOTTOM_MARGIN,
                        CharIcon[0].getIconWidth(), CharIcon[0].getIconHeight(), CharIcon[0]);
                ui.diaPanel.add(Chara);
                Timer t1 = new Timer(GameConstants.TRANSITION_SHORT_MS, e -> {
                    ui.resources.playSound("夜间死亡音效.wav");
                    Chara.setVisible(false);
                    if (CharIcon.length >= 2) {
                        JLabel Chara2 = UIFactory.makeLabel(LabelConst.Text_Label,
                                (GameConstants.WINDOW_WIDTH - CharIcon[1].getIconWidth()) / 2,
                                GameConstants.WINDOW_HEIGHT - CharIcon[1].getIconHeight() - GameConstants.CHAR_ICON_BOTTOM_MARGIN,
                                CharIcon[1].getIconWidth(), CharIcon[1].getIconHeight(), CharIcon[1]);
                        ui.diaPanel.add(Chara2);
                        ui.diaPanel.setComponentZOrder(Chara2, 1);
                    }
                    ui.resizeComponents();
                    ((Timer) e.getSource()).stop();
                });
                t1.start();
                Timer t2 = new Timer(GameConstants.TRANSITION_MEDIUM_MS, e -> {
                    ds.dc().dialogPanel.setVisible(true);
                    ds.dc().nextBtn.setVisible(true);
                    ds.dc().nextBtn.setEnabled(true);
                    typeTimer.start();
                    ((Timer) e.getSource()).stop();
                });
                t2.start();
            }
            ds.dc().nextBtn.setVisible(false);
            DialogueBox.finalize(ui, ds.dc());
        }
    }
}