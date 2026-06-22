import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GameSceneHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        switch (ui.currentScene) {
            case GAME_SCENE_SELECT: renderSelect(ui); break;
            case GAME_SCENE_NIGHT:  renderNight(ui);  break;
            case GAME_SCENE_DAY:    renderDay(ui);    break;
            default: break;
        }
    }

    private void renderSelect(UI ui) {
        ui.jPanel.removeAll();
        int x = 30;
        int y = 20;
        int width = 435;
        int height = 138;
        int x_div = 30 + width;
        int y_div = 30 + height;
        JButton btn1;
        for (int i = 1; i <= 7; i++) {
            final int j = i;
            btn1 = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,
                    x + (i / 5) * x_div, y + (i - 1) % 4 * y_div, width, height,
                    ui.resources.getImage("game" + i + ".png"));
            ui.jPanel.add(btn1);
            btn1.addActionListener(e -> {
                ui.levelName = "game" + j + "-1.png";
                ui.resources.playSound("click.wav");
                ui.mainLogic.start(peiyi.values()[j]);
                ui.ctx = ui.mainLogic.getGameContext();
                int playerSum = ui.ctx.getPlayerSum();
                ui.skillTargetPeople = new int[playerSum + 1][GameConstants.MAX_GAME_DAYS];
                ui.skillTargetNames = new String[playerSum + 1][GameConstants.MAX_GAME_DAYS];
                ui.skillTargetOrder = new int[playerSum + 1][GameConstants.MAX_GAME_DAYS];
                ui.claimedRolenum = new int[playerSum + 1][GameConstants.MAX_GAME_DAYS];
                if (DebugLogger.getInstance().isEnabled()) {
                    DebugLogger.log("启动后事件数量: " + ui.getEvents().size());
                    if (ui.getEvents().isEmpty()) {
                        DebugLogger.log("events为空，添加测试事件");
                        Event testEvent = new Event(EventName.yjsw, CharacterEnglishName.Beatrice);
                        ui.addEvent(testEvent);
                    }
                }
                ui.currentScene = UI.Scene.GAME_SCENE_NIGHT;
                ui.run();
            });
        }
        JButton backBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,
                1050, 560,
                GameConstants.RETURN_WIDTH * 6 / 10, GameConstants.RETURN_HEIGHT * 6 / 10,
                ui.resources.getImage("PVBtitile.png"));
        backBtn.addActionListener(e -> ui.transitionTo(UI.Scene.START_SCENE));
        ui.jPanel.add(backBtn);
        JLabel background = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT,
                ui.resources.getImage("title_base_resized.png"));
        ui.jPanel.add(background);
        ui.jFrame.setVisible(true);
        ui.resizeComponents();
    }

    private void renderNight(UI ui) {
        ui.specialEvent[0] = false;
        ui.isVote[0] = false;
        ui.isZhan[0] = false;
        ui.isHu[0] = false;
        ui.voteChosen.clear();
        ui.zhanChosen.clear();
        ui.huChosen.clear();
        if (!ui.getEvents().isEmpty()) {
            DebugLogger.log("事件不为空");
        }
        ui.jPanel.removeAll();
        JLabel label = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT,
                ui.resources.getImage("komorebi000night01.png"));
        ui.jPanel.add(label);
        ui.resizeComponents();
        ui.jFrame.setVisible(true);
        ui.resources.playBgm("");
        ui.resources.playSound("入夜音效.wav");
        Timer timer = new Timer(GameConstants.NIGHT_SCREEN_DURATION_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switch (ui.ctx.getEndResult()) {
                    case 0: ui.currentScene = UI.Scene.GAME_SCENE_DAY;    break;
                    case 1: ui.currentScene = UI.Scene.END_VILLAGE;       break;
                    case 2: ui.currentScene = UI.Scene.END_WOLF;          break;
                    case 3: ui.currentScene = UI.Scene.END_FOX;           break;
                }
                ((Timer) e.getSource()).stop();
                ui.run();
            }
        });
        timer.start();
    }

    private void renderDay(UI ui) {
        if (ui.ctx.getEndResult() >= 1 && ui.ctx.getEndResult() <= 3) {
            ui.jPanel.removeAll();
            ui.jPanel.revalidate();
            ui.jPanel.repaint();
            switch (ui.ctx.getEndResult()) {
                case 1: ui.currentScene = UI.Scene.END_VILLAGE; break;
                case 2: ui.currentScene = UI.Scene.END_WOLF;    break;
                case 3: ui.currentScene = UI.Scene.END_FOX;     break;
            }
            ui.run();
            return;
        }
        ui.jPanel.removeAll();
        DialogueBox.Components dc = DialogueBox.create(ui.resources, "komorebi002.png");
        ui.jPanel.add(dc.dialogPanel);
        String dayText = String.format(GameStrings.DAY_START_FORMAT, ui.ctx.getGameDay());
        dc.dialogPanel.setVisible(false);
        Timer typeTimer = UIHelpers.bindTypewriter(dc.dialogText, dayText, dc.nextBtn, () -> {
            ui.currentScene = UI.Scene.DIALOGUE_DEATH;
            ui.run();
        });
        typeTimer.stop();
        dc.dialogPanel.add(dc.nextBtn);
        dc.dialogPanel.add(dc.dialogText);
        dc.dialogPanel.add(dc.back);
        ui.resources.playSound("狼嚎音效.wav");
        Timer timer = new Timer(GameConstants.HOWL_TRANSITION_MS, e -> {
            dc.dialogPanel.setVisible(true);
            typeTimer.start();
            ((Timer) e.getSource()).stop();
        });
        timer.start();
        ui.jPanel.add(dc.dialogPanel);
        ui.jPanel.add(dc.background);
        ui.resizeComponents();
        ui.jFrame.setVisible(true);
    }
}