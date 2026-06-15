import javax.swing.*;
import java.awt.*;

public class DialogueDayDeathHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        if (ui.events.getFirst().eventname == EventName.wsw) {
            ui.resources.playSound("平和音效.wav");
            ui.diaPanel.removeAll();
            ui.diaPanel.setVisible(true);
            JLabel background = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                    GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT,
                    ui.resources.getImage("haikei3.png"));
            ImageIcon backIcon = ui.resources.getImage("messageframe.png");
            JPanel dialogPanel = PanelSimpleFactory.makePanel(PanelConst.Simple_Panel, 260, 450,
                    backIcon.getIconWidth(), backIcon.getIconHeight(), false, true);
            ui.diaPanel.add(dialogPanel);
            JLabel back = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                    backIcon.getIconWidth(), backIcon.getIconHeight(), backIcon);
            JTextArea dialogText = TextareaSimpleFactory.createBasicTextArea(Color.WHITE);
            dialogText.setBounds(20, 50, 710, 200);
            JButton nextBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 0, 0,
                    backIcon.getIconWidth(), backIcon.getIconHeight(), null, null);
            ui.resources.getEventText(ui.events.poll());
            ui.bindTypewriter(dialogText, "犠牲者はいませんでした。\n", nextBtn, () -> {
                if ((ui.gs.aliveCounter - 1) / 2 == 1) {
                    ui.resources.playBgm("西江紫堂 - 灯り無き眼光.wav");
                } else {
                    ui.resources.playBgm("Emotionally Unstable.wav");
                }
                ui.currentScene = UI.Scene.DIALOGUE_DAY;
                ui.run();
            });
            dialogPanel.setVisible(true);
            dialogPanel.add(nextBtn);
            dialogPanel.add(dialogText);
            dialogPanel.add(back);
            ui.diaPanel.add(background);
            ui.jPanel.add(ui.diaPanel);
            ui.jPanel.setComponentZOrder(ui.diaPanel, 0);
            ui.diaPanel.setVisible(true);
            ui.resizeComponents();
        } else {
            Event event = ui.events.poll();
            if (event == null) {
                ui.currentScene = UI.Scene.GAME_SCENE_VOTE;
                ui.run();
                return;
            }
            ui.jPanel.removeAll();
            ui.diaPanel.removeAll();
            ui.diaPanel.setVisible(true);
            JLabel background = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                    GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT,
                    ui.resources.getImage("haikei3.png"));
            ImageIcon backIcon = ui.resources.getImage("messageframe.png");
            JPanel dialogPanel = PanelSimpleFactory.makePanel(PanelConst.Simple_Panel, 260, 450,
                    backIcon.getIconWidth(), backIcon.getIconHeight(), false, true);
            ui.diaPanel.add(dialogPanel);
            ImageIcon[] CharIcon = ui.resources.getEventImage(event);
            JLabel back = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                    backIcon.getIconWidth(), backIcon.getIconHeight(), backIcon);
            JLabel nameLabel = LabelSimpleFactory.makeLabel(LabelConst.Text_Label, 40, 10, 1000, 30,
                    ui.uiComponentFactory.getCharacterFullName(event.ch1));
            dialogPanel.add(nameLabel);
            JTextArea dialogText = TextareaSimpleFactory.createBasicTextArea(Color.WHITE);
            dialogText.setBounds(20, 50, 710, 200);
            JButton nextBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 0, 0,
                    backIcon.getIconWidth(), backIcon.getIconHeight(), null, null);
            nextBtn.setVisible(false);
            nextBtn.setEnabled(false);
            String text = ui.resources.getEventText(event);
            Timer typeTimer = ui.bindTypewriter(dialogText, text, nextBtn, () -> {
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
            dialogPanel.setVisible(true);
            if (CharIcon.length != 0) {
                typeTimer.stop();
                dialogPanel.setVisible(false);
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
                    dialogPanel.setVisible(true);
                    nextBtn.setVisible(true);
                    nextBtn.setEnabled(true);
                    typeTimer.start();
                    ((Timer) e.getSource()).stop();
                });
                t2.start();
            }
            dialogPanel.add(nextBtn);
            dialogPanel.add(dialogText);
            dialogPanel.add(back);
            nextBtn.setVisible(false);
            ui.diaPanel.add(background);
            ui.jPanel.add(ui.diaPanel);
            ui.jPanel.setComponentZOrder(ui.diaPanel, 0);
            ui.diaPanel.setVisible(true);
            ui.resizeComponents();
        }
    }
}