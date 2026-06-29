import javax.swing.*;
import java.awt.*;

public class DialogueChuxingHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        ui.resources.playBgm("");
        Event event = ui.getEvents().poll();
        ui.jPanel.removeAll();
        UIHelpers.DialogueSetup ds = UIHelpers.prepareDialogueEvent(ui, "haikei.png", event);
        ui.resizeComponents();
        JPanel dialogPanel = ds.dc().dialogPanel;
        JTextArea dialogText = ds.dc().dialogText;
        JButton nextBtn = ds.dc().nextBtn;
        ImageIcon[] CharIcon = ds.charIcon();
        JLabel Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                (GameConstants.WINDOW_WIDTH - CharIcon[0].getIconWidth()) / 2,
                GameConstants.WINDOW_HEIGHT - CharIcon[0].getIconHeight() - GameConstants.CHAR_ICON_BOTTOM_MARGIN,
                CharIcon[0].getIconWidth(), CharIcon[0].getIconHeight(), CharIcon[0]);
        ui.diaPanel.add(Chara);
        Timer typeTimer = UIHelpers.bindTypewriter(dialogText, ds.text(), nextBtn, () -> {
            dialogPanel.setVisible(false);
            nextBtn.setVisible(false);
            Timer timer = new Timer(GameConstants.TRANSITION_MEDIUM_MS, e1 -> {
                dialogText.setText("");
                nextBtn.setVisible(true);
                String resultText;
                if (event.eventname == EventName.cxs) {
                    resultText = String.format(GameStrings.EXECUTED_FORMAT, event.ch1.name());
                } else if (event.eventname == EventName.hzsw) {
                    resultText = String.format(GameStrings.FOLLOW_DEATH_FORMAT, event.ch1.name());
                } else if (event.eventname == EventName.mzsw) {
                    resultText = String.format(GameStrings.CAT_CURSE_FORMAT, event.ch1.name());
                } else {
                    resultText = "";
                }
                UIHelpers.bindTypewriter(dialogText, resultText, nextBtn, () -> {
                    if (ui.getEvents().isEmpty() || (ui.getEvents().getFirst().eventname != EventName.cxs
                            && ui.getEvents().getFirst().eventname != EventName.hzsw
                            && ui.getEvents().getFirst().eventname != EventName.mzsw)) {
                        ui.currentScene = UI.Scene.GAME_SCENE_NIGHT;
                    }
                    ui.run();
                });
                dialogPanel.setVisible(true);
                ((Timer) e1.getSource()).stop();
            });
            timer.start();
            ui.resources.playSound("白天处刑音效.wav");
            Chara.setVisible(false);
            if (CharIcon.length >= 2) {
                JLabel Chara2 = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                        (GameConstants.WINDOW_WIDTH - CharIcon[1].getIconWidth()) / 2,
                        GameConstants.WINDOW_HEIGHT - CharIcon[1].getIconHeight() - GameConstants.CHAR_ICON_BOTTOM_MARGIN,
                        CharIcon[1].getIconWidth(), CharIcon[1].getIconHeight(), CharIcon[1]);
                ui.diaPanel.add(Chara2);
                ui.resizeComponents();
                ui.diaPanel.setComponentZOrder(Chara2, 1);
            }
        });
        DialogueBox.finalize(ui, ds.dc());
    }
}