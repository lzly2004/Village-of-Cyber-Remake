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
                event = seekEvent(ui, event, EventName.yhsl);
                ui.resources.playBgm("失败画面.wav");
                bgIcon = ui.resources.getImage("endFox.png");
                break;
            case END_WOLF:
                event = seekEvent(ui, event, EventName.krsl, EventName.rlsl);
                ui.resources.playBgm("失败画面.wav");
                bgIcon = ui.resources.getImage("endWolf.png");
                break;
            case END_VILLAGE:
            default:
                event = seekEvent(ui, event, EventName.crsl);
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

    private static final int MAX_SEEK_ITERATIONS = 200;

    private static Event seekEvent(UI ui, Event current, EventName... targets) {
        int iterations = 0;
        Event event = current;
        while (event != null && iterations < MAX_SEEK_ITERATIONS) {
            for (EventName t : targets) {
                if (event.eventname == t) return event;
            }
            event = ui.getEvents().poll();
            iterations++;
        }
        if (event == null) {
            DebugLogger.error("EndGameHandler: 事件队列为空，未找到目标事件");
        } else if (iterations >= MAX_SEEK_ITERATIONS) {
            DebugLogger.error("EndGameHandler: 超过最大迭代次数，未找到目标事件");
        }
        return event;
    }
}