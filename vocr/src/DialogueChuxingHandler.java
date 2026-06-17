import javax.swing.*;
import java.awt.*;

public class DialogueChuxingHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        ui.resources.playBgm("");
        Event event = ui.events.poll();
        ui.jPanel.removeAll();
        DialogueBox.Components dc = DialogueBox.setup(ui, "haikei.png");
        ui.resizeComponents();
        JPanel dialogPanel = dc.dialogPanel;
        JTextArea dialogText = dc.dialogText;
        JButton nextBtn = dc.nextBtn;
        ImageIcon[] CharIcon = ui.resources.getEventImage(event);
        JLabel Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                (1280 - CharIcon[0].getIconWidth()) / 2,
                720 - CharIcon[0].getIconHeight() - 30,
                CharIcon[0].getIconWidth(), CharIcon[0].getIconHeight(), CharIcon[0]);
        ui.diaPanel.add(Chara);
        JLabel nameLabel = LabelSimpleFactory.makeLabel(LabelConst.Text_Label, 40, 10, 1000, 30,
                ui.uiComponentFactory.getCharacterFullName(event.ch1));
        dialogPanel.add(nameLabel);
        String text = ui.resources.getEventText(event);
        final String[] fullText = {text};
        final int[] index = {0};
        Timer typeTimer = new Timer(GameConstants.TYPEWRITER_DELAY_MS, e -> {
            if (index[0] < fullText[0].length()) {
                dialogText.append(String.valueOf(fullText[0].charAt(index[0])));
                index[0]++;
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
        boolean[] isNext = {false};
        nextBtn.addActionListener(e -> {
            if (index[0] < fullText[0].length()) {
                dialogText.setText(fullText[0]);
                index[0] = fullText[0].length();
                typeTimer.stop();
            } else {
                dialogPanel.setVisible(false);
                nextBtn.setVisible(false);
                Timer timer = new Timer(GameConstants.TRANSITION_MEDIUM_MS, e1 -> {
                    dialogText.setText("");
                    nextBtn.setVisible(true);
                    CharacterKanjiName[] values = CharacterKanjiName.values();
                    if (event.eventname == EventName.cxs) {
                        fullText[0] = "投票の結果、" + values[ui.gs.gc[ui.chuxingWho].number].name() + "は処刑されました。";
                    } else if (event.eventname == EventName.hzsw) {
                        int num = 0;
                        for (int r = 1; r < ui.gs.gc.length; ++r) {
                            if (ui.gs.gc[r].whyDie == whyDie.dayhouzhui) {
                                num = ui.gs.gc[r].number;
                            }
                        }
                        fullText[0] = "" + values[num].name() + "後追いで死亡した。";
                    } else if (event.eventname == EventName.mzsw) {
                        int num = 0;
                        for (int r = 1; r < ui.gs.gc.length; ++r) {
                            if (ui.gs.gc[r].whyDie == whyDie.daymaozhou) {
                                num = ui.gs.gc[r].number;
                            }
                        }
                        fullText[0] = "" + values[num].name() + "猫の呪いによって死亡した。";
                    }
                    index[0] = 0;
                    typeTimer.start();
                    dialogPanel.setVisible(true);
                    isNext[0] = true;
                    ((Timer) e1.getSource()).stop();
                });
                timer.start();
                if (!isNext[0]) ui.resources.playSound("白天处刑音效.wav");
                Chara.setVisible(false);
                JLabel Chara2 = null;
                if (CharIcon.length >= 2) {
                    Chara2 = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                            (1280 - CharIcon[1].getIconWidth()) / 2,
                            720 - CharIcon[1].getIconHeight() - 30,
                            CharIcon[1].getIconWidth(), CharIcon[1].getIconHeight(), CharIcon[1]);
                }
                if (isNext[0]) {
                    if (ui.events.isEmpty() || (ui.events.getFirst().eventname != EventName.cxs
                            && ui.events.getFirst().eventname != EventName.hzsw
                            && ui.events.getFirst().eventname != EventName.mzsw)) {
                        ui.currentScene = UI.Scene.GAME_SCENE_NIGHT;
                    }
                    if (Chara2 != null) Chara2.setVisible(false);
                    ui.run();
                }
                if (Chara2 != null) {
                    ui.diaPanel.add(Chara2);
                    ui.resizeComponents();
                    ui.diaPanel.setComponentZOrder(Chara2, 1);
                }
            }
        });
        typeTimer.start();
        DialogueBox.finalize(ui, dc);
    }
}