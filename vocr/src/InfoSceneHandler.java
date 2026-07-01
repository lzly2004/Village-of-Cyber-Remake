// InfoSceneHandler.java - 信息界面场景处理器
import javax.swing.*;
import java.awt.*;

public class InfoSceneHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        UI.Scene scene = ui.currentScene;
        if (scene == UI.Scene.INFO_SCENE) {
            renderMain(ui);
        } else if (isFirstLevel(scene)) {
            renderFirst(ui, scene);
        } else {
            renderSecond(ui, scene);
        }
    }

    private boolean isFirstLevel(UI.Scene scene) {
        return scene == UI.Scene.INFO_SCENE_1 || scene == UI.Scene.INFO_SCENE_2
            || scene == UI.Scene.INFO_SCENE_3 || scene == UI.Scene.INFO_SCENE_4
            || scene == UI.Scene.INFO_SCENE_5;
    }

    private void renderMain(UI ui) {
        ui.jPanel.removeAll();
        ImageIcon btnNext = ui.resources.getImage("avg_button2.png");
        JButton btn_next[] = new JButton[6];
        for (int i = 1; i <= 5; i++) {
            final int j = i;
            btn_next[i] = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 80, 10 + 70 * i, 220, 50,
                    ui.resources.getHelpText("Info" + i + ".txt"), btnNext);
            btn_next[i].setHorizontalTextPosition(SwingConstants.CENTER);
            btn_next[i].addActionListener(e -> ui.transitionTo(UI.Scene.valueOf("INFO_SCENE_" + j)));
            ui.jPanel.add(btn_next[i]);
        }
        JLabel backgroundLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT,
                ui.resources.getImage("PVBG.png"));
        JLabel backgroundLabel_2 = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 10, 10, 980, 660,
                ui.resources.getImage("avg1_resized(3).png"));
        JButton btnMenu = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1050, 560,
                GameConstants.RETURN_WIDTH, GameConstants.RETURN_HEIGHT,
                ui.resources.getImage("PVBtitile.png"));
        btnMenu.addActionListener(e -> ui.transitionTo(UI.Scene.START_SCENE));
        ui.jPanel.add(btnMenu);
        ui.jPanel.add(backgroundLabel_2);
        ui.jPanel.add(backgroundLabel);
        ui.getJFrame().setVisible(true);
        ui.resizeComponents();
    }

    private record InfoBackground(UI ui, JLabel bg, JLabel bg2, JButton menu, JButton back) {}

    private InfoBackground createInfoBackground(UI ui, UI.Scene backTarget) {
        ui.jPanel.removeAll();
        JLabel bg = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT,
                ui.resources.getImage("PVBG.png"));
        JLabel bg2 = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 10, 10, 980, 660,
                ui.resources.getImage("avg1_resized(3).png"));
        JButton menu = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1050, 560,
                GameConstants.RETURN_WIDTH, GameConstants.RETURN_HEIGHT,
                ui.resources.getImage("PVBtitile.png"));
        menu.addActionListener(e -> ui.transitionTo(UI.Scene.START_SCENE));
        JButton back = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1050, 400,
                GameConstants.RETURN_WIDTH, GameConstants.RETURN_HEIGHT,
                ui.resources.getImage("PVBreturn.png"));
        back.addActionListener(e -> ui.transitionTo(backTarget));
        return new InfoBackground(ui, bg, bg2, menu, back);
    }

    private void finishInfoRender(InfoBackground ib) {
        ib.ui.jPanel.add(ib.menu);
        ib.ui.jPanel.add(ib.back);
        ib.ui.jPanel.add(ib.bg2);
        ib.ui.jPanel.add(ib.bg);
        ib.ui.getJFrame().setVisible(true);
        ib.ui.resizeComponents();
    }

    private void renderSecond(UI ui, UI.Scene scene) {
        InfoBackground ib = createInfoBackground(ui, scene.FatherScene());
        JTextArea dialogText = TextareaSimpleFactory.createBoldTitleTextArea(Color.WHITE, 24,
                ui.resources.getHelpText(scene.toString()));
        JScrollPane scrollPane = new JScrollPane(dialogText);
        scrollPane.setBounds(50, 50, 880, 630);
        UIHelpers.configureScrollPane(scrollPane);
        ui.jPanel.add(scrollPane);
        finishInfoRender(ib);
    }

    private void renderFirst(UI ui, UI.Scene scene) {
        InfoBackground ib = createInfoBackground(ui, UI.Scene.INFO_SCENE);
        int subSum = scene.SubInfoSum();
        ImageIcon btnNext[] = new ImageIcon[subSum + 1];
        JButton btn_next[] = new JButton[subSum + 1];
        for (int i = 1; i <= subSum; i++) {
            final int currentIndex = i;
            btnNext[i] = ui.resources.getImage("avg_button2.png");
            btn_next[i] = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,
                    80, 70 * i + 10, 222, 50,
                    ui.resources.getHelpText("Info" + scene.FirstInfoNum() + "-" + i + ".txt"), btnNext[i]);
            btn_next[i].setHorizontalTextPosition(SwingConstants.CENTER);
            btn_next[i].addActionListener(e -> ui.transitionTo(UI.Scene.values()[scene.ordinal() + currentIndex]));
            ui.jPanel.add(btn_next[i]);
        }
        finishInfoRender(ib);
    }
}