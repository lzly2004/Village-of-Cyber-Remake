import javax.swing.*;
import java.awt.*;

public class DialogueDayDeathHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        if (ui.events.getFirst().eventname == EventName.wsw) {
            ui.resources.playSound("平和音效.wav");
            DialogueBox.Components dc = DialogueBox.setup(ui, "haikei3.png");
            ui.resources.getEventText(ui.events.poll());
            UIHelpers.bindTypewriter(dc.dialogText, "犠牲者はいませんでした。\n", dc.nextBtn, () -> {
                if ((ui.gs.aliveCounter - 1) / 2 == 1) {
                    ui.resources.playBgm("西江紫堂 - 灯り無き眼光.wav");
                } else {
                    ui.resources.playBgm("Emotionally Unstable.wav");
                }
                ui.currentScene = UI.Scene.DIALOGUE_DAY;
                ui.run();
            });
            dc.dialogPanel.setVisible(true);
            DialogueBox.finalize(ui, dc);
        } else {
            Event event = ui.events.poll();
            if (event == null) {
                ui.currentScene = UI.Scene.GAME_SCENE_VOTE;
                ui.run();
                return;
            }
            ui.jPanel.removeAll();
            DialogueBox.Components dc = DialogueBox.setup(ui, "haikei3.png");
            ImageIcon[] CharIcon = ui.resources.getEventImage(event);
            JLabel nameLabel = LabelSimpleFactory.makeLabel(LabelConst.Text_Label, 40, 10, 1000, 30,
                    ui.uiComponentFactory.getCharacterFullName(event.ch1));
            dc.dialogPanel.add(nameLabel);
            dc.nextBtn.setVisible(false);
            dc.nextBtn.setEnabled(false);
            String text = ui.resources.getEventText(event);
            Timer typeTimer = UIHelpers.bindTypewriter(dc.dialogText, text, dc.nextBtn, () -> {
                if (ui.events.isEmpty() || ui.events.getFirst().eventname != EventName.yjsw) {
                    if ((ui.gs.aliveCounter - 1) / 2 == 1) {
                        ui.resources.playBgm("西江紫堂 - 灯り無き眼光.wav");
                    } else {
                        ui.resources.playBgm("Emotionally Unstable.wav");
                    }
                    ui.currentScene = UI.Scene.DIALOGUE_DAY;
                }
                ui.run();
            });
            dc.dialogPanel.setVisible(true);
            if (CharIcon.length != 0) {
                typeTimer.stop();
                dc.dialogPanel.setVisible(false);
                JLabel Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                        (1280 - CharIcon[0].getIconWidth()) / 2,
                        720 - CharIcon[0].getIconHeight() - 30,
                        CharIcon[0].getIconWidth(), CharIcon[0].getIconHeight(), CharIcon[0]);
                ui.diaPanel.add(Chara);
                Timer t1 = new Timer(GameConstants.TRANSITION_SHORT_MS, e -> {
                    ui.resources.playSound("夜间死亡音效.wav");
                    Chara.setVisible(false);
                    if (CharIcon.length >= 2) {
                        JLabel Chara2 = LabelSimpleFactory.makeLabel(LabelConst.Text_Label,
                                (1280 - CharIcon[1].getIconWidth()) / 2,
                                720 - CharIcon[1].getIconHeight() - 30,
                                CharIcon[1].getIconWidth(), CharIcon[1].getIconHeight(), CharIcon[1]);
                        ui.diaPanel.add(Chara2);
                        ui.diaPanel.setComponentZOrder(Chara2, 1);
                    }
                    ui.resizeComponents();
                    ((Timer) e.getSource()).stop();
                });
                t1.start();
                Timer t2 = new Timer(GameConstants.TRANSITION_MEDIUM_MS, e -> {
                    dc.dialogPanel.setVisible(true);
                    dc.nextBtn.setVisible(true);
                    dc.nextBtn.setEnabled(true);
                    typeTimer.start();
                    ((Timer) e.getSource()).stop();
                });
                t2.start();
            }
            dc.nextBtn.setVisible(false);
            DialogueBox.finalize(ui, dc);
        }
    }
}