import javax.swing.*;
import java.awt.*;

public class EndGameHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        UI.Scene scene = ui.currentScene;
        Event event = ui.getEvents().poll();
        ImageIcon bgIcon;
        switch (scene) {
            case END_FOX:
                while (event.eventname != EventName.yhsl) {
                    event = ui.getEvents().poll();
                }
                ui.resources.playBgm("失败画面.wav");
                bgIcon = ui.resources.getImage("endFox.png");
                break;
            case END_WOLF:
                while (event.eventname != EventName.krsl && event.eventname != EventName.rlsl) {
                    event = ui.getEvents().poll();
                }
                ui.resources.playBgm("失败画面.wav");
                bgIcon = ui.resources.getImage("endWolf.png");
                break;
            case END_VILLAGE:
            default:
                while (event.eventname != EventName.crsl) {
                    event = ui.getEvents().poll();
                }
                ui.resources.playBgm("胜利画面.wav");
                bgIcon = ui.resources.getImage("endVillage.png");
                break;
        }
        DialogueBox.Components dc = DialogueBox.setupWithIcon(ui, bgIcon);
        JLabel nameLabel = LabelSimpleFactory.makeLabel(LabelConst.Text_Label, 20, 10, 1000, 30,
                ui.uiComponentFactory.getCharacterFullName(event.ch1));
        dc.dialogPanel.add(nameLabel);
        Timer typeTimer = UIHelpers.bindTypewriter(dc.dialogText, ui.resources.getEventText(event), dc.nextBtn, () -> {
            dc.dialogPanel.setVisible(false);
            switch (scene) {
                case END_VILLAGE: ui.resources.playSound("村人胜利音效.wav"); break;
                case END_WOLF:    ui.resources.playSound("人狼胜利音效.wav"); break;
                case END_FOX:     ui.resources.playSound("妖狐胜利音效.wav"); break;
            }
            Timer t = new Timer(GameConstants.ENDING_DISPLAY_MS, e1 -> {
                ui.currentScene = UI.Scene.END_ANIME;
                ui.run();
                ((Timer) e1.getSource()).stop();
            });
            t.start();
        });
        dc.dialogPanel.setVisible(true);
        ImageIcon[] CharIcon = ui.resources.getEventImage(event);
        JLabel Chara = new JLabel();
        if (!ui.linkIcon.isEmpty()) {
            UIHelpers.renderLinkIconPair(ui, CharIcon);
        } else {
            UIHelpers.renderDialogueCharacter(ui, event, CharIcon);
        }
        DialogueBox.finalize(ui, dc);
    }
}