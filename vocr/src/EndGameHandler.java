import javax.swing.*;
import java.awt.*;

public class EndGameHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        UI.Scene scene = ui.currentScene;
        Event event = ui.events.poll();
        ImageIcon bgIcon;
        switch (scene) {
            case END_FOX:
                while (event.eventname != EventName.yhsl) {
                    event = ui.events.poll();
                }
                ui.resources.playBgm("失败画面.wav");
                bgIcon = ui.resources.getImage("endFox.png");
                break;
            case END_WOLF:
                while (event.eventname != EventName.krsl && event.eventname != EventName.rlsl) {
                    event = ui.events.poll();
                }
                ui.resources.playBgm("失败画面.wav");
                bgIcon = ui.resources.getImage("endWolf.png");
                break;
            case END_VILLAGE:
            default:
                while (event.eventname != EventName.crsl) {
                    event = ui.events.poll();
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
            Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, CharIcon[0]);
            boolean isLinked = false;
            switch (event.eventname) {
                case gyfo1:
                case qfjc5:
                case zjgh8b:
                case zjgb8:
                case gprz11p:
                case zcrh12:
                    isLinked = true;
                    break;
            }
            if (isLinked) {
                Chara.setBounds(300, 720 - CharIcon[0].getIconHeight() - 30,
                        CharIcon[0].getIconWidth(), CharIcon[0].getIconHeight());
                ui.linkIcon.add(CharIcon[0]);
            } else {
                Chara.setBounds((1280 - CharIcon[0].getIconWidth()) / 2,
                        720 - CharIcon[0].getIconHeight() - 30,
                        CharIcon[0].getIconWidth(), CharIcon[0].getIconHeight());
            }
            ui.diaPanel.add(Chara);
            ui.resizeComponents();
        }
        DialogueBox.finalize(ui, dc);
    }
}